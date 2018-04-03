/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.accio.runtime

import java.nio.file.{Files, Path, Paths}
import java.util.UUID

import com.google.common.annotations.VisibleForTesting
import com.google.inject.Inject
import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.api.{DataTypes, Values}
import fr.cnrs.liris.accio.sdk.{OpContext, Operator}

import scala.util.control.NonFatal

/**
 * Operator execution options.
 *
 * @param useProfiler  Whether to profile code execution.
 * @param cleanSandbox Whether to clean sandbox immediately after operator completion.
 */
case class OpExecutorOpts(useProfiler: Boolean, cleanSandbox: Boolean = true)

/**
 * Entry point for executing an operator from a payload. It manages the whole lifecycle of
 * instantiating an operator, executing it, collecting its outputs and returning them.
 *
 * As stated in the [[OpPayload]] and [[OpResult]] classes' description, the main goal here is to
 * enforce reproducibility of execution. Given the same inputs, the result should be the same
 * (modulo some metrics, as timing or load information may vary and are not controllable). This is
 * why this class knows and should never know anything about runs, graphs or nodes.
 *
 * @param operators Available operators.
 */
@Inject
final class OpExecutor @Inject()(operators: Set[OpMeta]) extends Logging {
  // Because the executor is designed to run inside a sandbox, we simply use current directory as
  // temporary path for the operator executor.
  private[this] var workDir = Paths.get(".")

  @VisibleForTesting
  private[accio] def setWorkDir(path: Path): Unit = {
    workDir = path
  }

  /**
   * Execute an operator. A challenge is to avoid throwing unchecked exceptions. The only exceptions that can be
   * thrown are (normally) declared and can be caught by client code.
   *
   * @param payload Operator payload.
   * @param opts    Executor options.
   * @return Result of the operator execution.
   */
  def execute(payload: OpPayload, opts: OpExecutorOpts): OpResult = {
    operators.find(_.defn.name == payload.op) match {
      case Some(opMeta) =>
        val operator = opMeta.opClass.newInstance()
        execute(operator, opMeta, payload, opts)
      case None => throw new IllegalArgumentException(s"Unknown operator: ${payload.op}")
    }
  }

  /**
   * Execute the instantiated operator.
   *
   * @param operator Operator instance.
   * @param opMeta   Operator metadata.
   * @param payload  Operator payload.
   * @param opts     Executor options.
   * @tparam In Operator input type.
   * @return Result of the operator execution.
   */
  private def execute[In](operator: Operator[In, _], opMeta: OpMeta, payload: OpPayload, opts: OpExecutorOpts) = {
    val sandboxDir = workDir.resolve(UUID.randomUUID().toString)
    Files.createDirectories(sandboxDir.resolve("outputs"))
    Files.createDirectories(sandboxDir.resolve("inputs"))

    val inputs = payload.inputs.toMap
    val in = createInput(opMeta, inputs).asInstanceOf[In]

    val maybeSeed = if (opMeta.defn.unstable) Some(payload.seed) else None
    val ctx = new OpContext(maybeSeed, sandboxDir.resolve("outputs"))
    val profiler = if (opts.useProfiler) new JvmProfiler else NullProfiler

    // The actual operator is the only profiled section. The outcome is either an output object or an exception.
    logger.debug(s"Starting operator ${opMeta.defn.name} (sandbox in ${sandboxDir.toAbsolutePath})")
    val res = profiler.profile {
      try {
        Some(operator.execute(in, ctx))
      } catch {
        case NonFatal(e) =>
          logger.warn(s"Unexpected error while executing operator ${opMeta.defn.name}", e)
          None
      }
    }
    logger.debug(s"Completed operator ${opMeta.defn.name}")

    // We convert the outcome into an exit code, artifacts, metrics and possibly and error.
    val artifacts = res.toSet.flatMap((out: Any) => extractArtifacts(opMeta, out))
    val metrics = profiler.metrics
    val exitCode = if (res.isDefined) 0 else 1

    if (opts.cleanSandbox) {
      // Sandbox directory can now be deleted. Caveat: If there was a fatal error before, this line will never be
      // reached and it will not be deleted.
      // TODO: clean only files that are not referenced in artifacts.
      // logger.debug(s"Cleaned sandbox")
    }

    OpResult(exitCode, artifacts, metrics)
  }

  /**
   * Create the input for an operator.
   *
   * @param opMeta Operator metadata.
   * @param inputs Mapping between input arguments and values.
   */
  private def createInput(opMeta: OpMeta, inputs: Map[String, Value]): Any = {
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
                    throw new IllegalArgumentException(s"Missing required input ${argDef.name}")
                  }
                  // An optional argument always accept None as value.
                  None
              }
            case Some(v) =>
              val normalizedValue = Values
                .as(v, argDef.kind)
                .getOrElse(throw new IllegalArgumentException(s"Invalid input type for ${argDef.name}: ${DataTypes.stringify(v.kind)}"))
              val value = Values.decode(normalizedValue, argDef.kind)
              if (argDef.isOptional) Some(value) else value
          }
        }
        inClass.getConstructors.head.newInstance(ctorArgs.map(_.asInstanceOf[AnyRef]): _*)
    }
  }

  /**
   * Extract artifacts from the output of an operator.
   *
   * @param opMeta Operator metadata.
   * @param out    Operator output.
   */
  private def extractArtifacts(opMeta: OpMeta, out: Any): Set[Artifact] = {
    opMeta.outClass match {
      case None => Set.empty
      case Some(outClass) =>
        opMeta.defn.outputs.map { argDef =>
          val rawValue = outClass.getMethod(argDef.name).invoke(out)
          // The exception should never be thrown, if everything else is in place. It seems indeed
          // extremely difficult for the client to return something over the wrong type, because it
          // is checked by the JVM (unless he does some dangerous type cast).
          // If this exception is thrown, it is likely there is an error somewhere else, while
          // inferring the various types.
          Values
            .encode(rawValue, argDef.kind)
            .map(value => Artifact(argDef.name, value))
            .getOrElse(throw new RuntimeException(s"Invalid output for ${argDef.name}: $rawValue"))
        }.toSet
    }
  }
}
