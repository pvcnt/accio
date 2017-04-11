/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnrs.liris.accio.core.framework

import java.nio.file.{Files, Path, Paths}
import java.util.UUID

import com.google.common.annotations.VisibleForTesting
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.api.Errors
import fr.cnrs.liris.accio.core.api.thrift._
import fr.cnrs.liris.accio.core.filesystem.FileSystem
import fr.cnrs.liris.accio.core.operator.{OpContext, Operator}
import fr.cnrs.liris.common.util.FileUtils
import fr.cnrs.liris.dal.core.api.{AtomicType, Value, Values}
import fr.cnrs.liris.dal.core.io.{Decoder, Encoder}
import fr.cnrs.liris.dal.core.sparkle.SparkleEnv

import scala.util.control.NonFatal

/**
 * Operator execution options.
 *
 * @param useProfiler  Whether to profile code execution.
 * @param cleanSandbox Whether to clean sandbox immediately after operator completion.
 */
case class OpExecutorOpts(useProfiler: Boolean, cleanSandbox: Boolean = true)

/**
 * Exception thrown when an input is missing for an operator.
 *
 * @param op  Operator name.
 * @param arg Input port name.
 */
class MissingOpInputException(val op: String, val arg: String) extends Exception(s"Missing required input of $op operator: $arg")

/**
 * Exception thrown when an operator is unknown.
 *
 * @param op Operator name.
 */
class UnknownOpException(val op: String) extends RuntimeException(s"Unknown operator: $op")

/**
 * Entry point for executing an operator from a payload. It manages the whole lifecycle of instantiating an operator,
 * executing it, collecting its outputs and returning them. It also manages downloading and uploading artifacts that
 * need to be (e.g., datasets).
 *
 * As stated in the [[OpPayload]] and [[OpResult]] classes' description, the main goal here is to enforce
 * reproducibility of execution. Given the same inputs, the result should be the same (modulo maybe some metrics, as
 * timing or load information may vary and are not controllable). This is why this class knows and should never know
 * anything about runs, graphs or nodes.
 *
 * @param opRegistry Operator registry.
 * @param opFactory  Operator factory.
 * @param filesystem Distributed filesystem, to read inputs and store outputs.
 * @param env        Sparkle environment.
 * @param encoders   Encoders available to write data to CSV files.
 * @param decoders   Decoders available to read data from CSV files.
 */
@Inject
final class OpExecutor @Inject()(
  opRegistry: RuntimeOpRegistry,
  opFactory: OpFactory,
  filesystem: FileSystem,
  env: SparkleEnv,
  encoders: Set[Encoder[_]],
  decoders: Set[Decoder[_]])
  extends StrictLogging {

  // Because the executor is designed to run inside a sandbox, we simply use current directory as temporary path
  // for the operator executor.
  private[this] var workDir = Paths.get(".")

  @VisibleForTesting
  private[framework] def setWorkDir(path: Path): Unit = {
    workDir = path
  }

  /**
   * Execute an operator. A challenge is to avoid throwing unchecked exceptions. The only exceptions that can be
   * thrown are (normally) declared and can be caught by client code.
   *
   * @param payload Operator payload.
   * @param opts    Executor options.
   * @throws UnknownOpException      If the operator is unknown.
   * @throws MissingOpInputException If an input is missing.
   * @return Result of the operator execution.
   */
  @throws[UnknownOpException]
  @throws[MissingOpInputException]
  def execute(payload: OpPayload, opts: OpExecutorOpts): OpResult = {
    if (!opRegistry.contains(payload.op)) {
      throw new UnknownOpException(payload.op)
    }
    val opDef = opRegistry(payload.op)
    val operator = opFactory.create(opDef)
    execute(operator, opDef, payload, opts)
  }

  /**
   * Execute the instantiated operator.
   *
   * @param operator Operator instance.
   * @param opDef    Operator definition (consistent with the operator instance).
   * @param payload  Operator payload.
   * @param opts     Executor options.
   * @tparam In Operator input type.
   * @return Result of the operator execution.
   */
  private def execute[In](operator: Operator[In, _], opDef: OpDef, payload: OpPayload, opts: OpExecutorOpts) = {
    val sandboxDir = workDir.resolve(UUID.randomUUID().toString)
    Files.createDirectories(sandboxDir.resolve("outputs"))
    Files.createDirectories(sandboxDir.resolve("inputs"))

    val inputs = downloadInputs(opDef, sandboxDir, payload.inputs.toMap)
    val in = createInput(opDef, inputs).asInstanceOf[In]

    val maybeSeed = if (opDef.unstable) Some(payload.seed) else None
    val ctx = new OpContext(maybeSeed, sandboxDir.resolve("outputs"), env, decoders, encoders)
    val profiler = if (opts.useProfiler) new JvmProfiler else NullProfiler

    // The actual operator is the only profiled section. The outcome is either an output object or an exception.
    logger.debug(s"Starting operator ${opDef.name} (sandbox in ${sandboxDir.toAbsolutePath})")
    val res = profiler.profile {
      try {
        Left(operator.execute(in, ctx))
      } catch {
        case NonFatal(e) => Right(Errors.create(e))
        case e: OutOfMemoryError =>
          System.gc()
          Right(Errors.create(e))
      }
    }
    logger.debug(s"Completed operator ${opDef.name}")

    // We convert the outcome into an exit code, artifacts, metrics and possibly and error.
    val (artifacts, error) = res match {
      case Left(out) => (uploadArtifacts(extractArtifacts(opDef, out), payload.cacheKey), None)
      case Right(ex) => (Set.empty[Artifact], Some(ex))
    }
    val metrics = profiler.metrics
    val exitCode = error.map(_.root.classifier.hashCode).getOrElse(0)

    if (opts.cleanSandbox) {
      // Sandbox directory can now be deleted. Caveat: If there was a fatal error before, this line will never be
      // reached and it will not be deleted.
      FileUtils.safeDelete(sandboxDir)
      logger.debug(s"Cleaned sandbox")
    }

    OpResult(exitCode, error, artifacts, metrics)
  }

  /**
   * Create the input for an operator.
   *
   * @param opDef  Operator definition.
   * @param inputs Mapping between input arguments and values.
   */
  private def createInput(opDef: OpDef, inputs: Map[String, Value]): Any = {
    val opMeta = opRegistry.meta(opDef.name)
    opMeta.inClass match {
      case None => Unit.box(Unit)
      case Some(inClass) =>
        val ctorArgs = opMeta.defn.inputs.map { argDef =>
          inputs.get(argDef.name) match {
            case None =>
              argDef.defaultValue match {
                case Some(defaultValue) =>
                  // Optional arguments (i.e., with a default value) are never Option[_]'s.
                  Values.decode(defaultValue, argDef.kind)
                case None =>
                  if (!argDef.isOptional) {
                    throw new MissingOpInputException(opDef.name, argDef.name)
                  }
                  // An optional argument always accept None as value.
                  None
              }
            case Some(v) =>
              val value = Values.decode(v, argDef.kind)
              if (argDef.isOptional) Some(value) else value
          }
        }
        inClass.getConstructors.head.newInstance(ctorArgs.map(_.asInstanceOf[AnyRef]): _*)
    }
  }

  /**
   * Extract artifacts from the output of an operator.
   *
   * @param opDef Operator definition.
   * @param out   Operator output.
   */
  private def extractArtifacts(opDef: OpDef, out: Any): Set[Artifact] = {
    val opMeta = opRegistry.meta(opDef.name)
    opMeta.outClass match {
      case None => Set.empty
      case Some(outClass) =>
        opMeta.defn.outputs.map { argDef =>
          val value = outClass.getMethod(argDef.name).invoke(out)
          Artifact(argDef.name, Values.encode(value, argDef.kind))
        }.toSet
    }
  }

  /**
   * Download inputs that need to be downloaded inside the sandbox directory, i.e., those that hold a reference to a
   * remote storage.
   *
   * @param opDef      Operator definition.
   * @param sandboxDir Sandbox directory.
   * @param inputs     Operator inputs.
   * @return Rewritten list of inputs, taking into account artifacts final local destination.
   */
  private def downloadInputs(opDef: OpDef, sandboxDir: Path, inputs: Map[String, Value]): Map[String, Value] = {
    inputs.map { case (name, value) =>
      val inputDef = opDef.inputs.find(_.name == name).get
      val newValue = inputDef.kind.base match {
        case AtomicType.Dataset =>
          val dataset = Values.decodeDataset(value)
          val dst = sandboxDir.resolve("inputs").resolve(name)
          logger.debug(s"Downloading inputs/$name...")
          filesystem.read(dataset.uri, dst)
          Values.encodeDataset(dataset.copy(uri = dst.toAbsolutePath.toString))
        case _ => value
      }
      name -> newValue
    }
  }

  /**
   * Upload artifacts that need to be uploaded, i.e., those that hold a reference to local storage.
   *
   * @param artifacts Artifacts produced by the operator.
   * @param cacheKey  Cache key for the outputs.
   * @return Rewritten list of artifacts, taking into account artifacts' final remote destination.
   */
  private def uploadArtifacts(artifacts: Set[Artifact], cacheKey: CacheKey): Set[Artifact] = {
    artifacts.map { artifact =>
      artifact.value.kind.base match {
        case AtomicType.Dataset =>
          val key = s"${cacheKey.hash}/${UUID.randomUUID}"
          val dataset = Values.decodeDataset(artifact.value)
          logger.debug(s"Uploading outputs/${artifact.name} under $key...")
          val newUri = filesystem.write(Paths.get(dataset.uri), key)
          artifact.copy(value = Values.encodeDataset(dataset.copy(uri = newUri)))
        case _ => artifact
      }
    }
  }
}
