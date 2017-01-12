/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.application

import java.nio.file.{Files, Path}

import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.api.{OpContext, Operator}
import fr.cnrs.liris.accio.core.domain.{ErrorFactory, OpFactory, RuntimeOpRegistry, Values}
import fr.cnrs.liris.accio.core.domain.{Artifact, OpPayload, OpResult, Value}
import fr.cnrs.liris.common.util.FileUtils

import scala.util.control.NonFatal

case class OpExecutorOpts(useProfiler: Boolean)

/**
 *
 * @param opRegistry
 * @param opFactory
 * @param uploader
 */
class OpExecutor(opRegistry: RuntimeOpRegistry, opFactory: OpFactory, uploader: Uploader) extends StrictLogging {
  /**
   * Execute an operator.
   *
   * @param payload Operator payload.
   * @param opts    Executor options.
   * @return Result of the operator execution.
   */
  def execute(payload: OpPayload, opts: OpExecutorOpts): OpResult = {
    val operator = opFactory.create(opRegistry(payload.op))
    execute(operator, payload, opts)
  }

  /**
   * Execute an operator.
   *
   * @param operator Operator instance.
   * @param payload  Operator payload.
   * @param opts     Executor options.
   * @tparam In Operator input type.
   * @return Result of the operator execution.
   */
  private def execute[In](operator: Operator[In, _], payload: OpPayload, opts: OpExecutorOpts) = {
    //TODO: download artifacts before executing operator.
    //TODO: upload artifacts after executing operator.
    val in = createInput(payload.op, payload.inputs).asInstanceOf[In]
    val maybeSeed = if (operator.isUnstable(in)) Some(payload.seed) else None
    val sandboxDir = Files.createTempDirectory("accio-executor-")
    val ctx = new OpContext(maybeSeed, sandboxDir)
    val profiler = if (opts.useProfiler) new JvmProfiler else NullProfiler

    // The actual operator is the only profiled section. The outcome is either an output object or an exception.
    val res = profiler.profile {
      try {
        Left(operator.execute(in, ctx))
      } catch {
        case NonFatal(e) => Right(ErrorFactory.create(e))
      }
    }

    // We convert the outcome into an exit code, artifacts, metrics and possibly and error.
    val (artifacts, error) = res match {
      case Left(out) => (extractArtifacts(payload.op, out), None)
      case Right(ex) => (Set.empty[Artifact], Some(ex))
    }
    val metrics = profiler.metrics
    val exitCode = error.map(_.root.classifier.hashCode).getOrElse(0)

    // Sandbox directory can now be deleted. Caveat: If there was a fatal error before, this line will never be
    // reached and it will not be deleted.
    FileUtils.safeDelete(sandboxDir)

    OpResult(exitCode, error, artifacts, metrics)
  }

  /**
   * Create the input for an operator.
   *
   * @param opName Operator name.
   * @param inputs Mapping between input arguments and values.
   */
  private def createInput(opName: String, inputs: collection.Map[String, Value]): Any = {
    val opMeta = opRegistry.meta(opName)
    opMeta.inClass match {
      case None => Unit.box(Unit)
      case Some(inClass) =>
        val ctorArgs = opMeta.defn.inputs.map { argDef =>
          inputs.get(argDef.name) match {
            case None =>
              argDef.defaultValue match {
                case Some(defaultValue) => Values.decode(defaultValue, argDef.kind)
                case None => throw new IllegalArgumentException(s"Missing required input: ${argDef.name}")
              }
            case Some(value) => Values.decode(value, argDef.kind)
          }
        }
        inClass.getConstructors.head.newInstance(ctorArgs.map(_.asInstanceOf[AnyRef]): _*)
    }
  }

  /**
   * Extract artifacts from the output of an operator.
   *
   * @param opName Operator name.
   * @param out    Operator output.
   */
  private def extractArtifacts(opName: String, out: Any): Set[Artifact] = {
    val opMeta = opRegistry.meta(opName)
    opMeta.outClass match {
      case None => Set.empty
      case Some(outClass) =>
        opMeta.defn.outputs.map { argDef =>
          val value = outClass.getMethod(argDef.name).invoke(out)
          Artifact(argDef.name, argDef.kind, Values.encode(value, argDef.kind))
        }.toSet
    }
  }
}
