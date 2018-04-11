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
import fr.cnrs.liris.accio.sdk.{OpContext, ScalaOperator}

import scala.util.control.NonFatal

/**
 * Entry point for executing an operator from a payload. It manages the whole lifecycle of
 * instantiating an operator, executing it, collecting its outputs and returning them.
 *
 * The main goal here is to enforce reproducibility of the execution. Given the same inputs, the
 * result should be the same
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
  private[accio] def setWorkDir(path: Path): Unit = workDir = path

  /**
   * Execute an operator.
   *
   * @param payload Operator payload.
   * @return Result of the operator execution.
   */
  def execute(payload: OpPayload): OpResult = {
    operators.find(_.defn.name == payload.op) match {
      case Some(opMeta) => execute(opMeta, payload)
      case None => throw new IllegalArgumentException(s"Unknown operator: ${payload.op}")
    }
  }

  private def execute(opMeta: OpMeta, payload: OpPayload) = {
    val sandboxDir = workDir.resolve(UUID.randomUUID().toString)
    Files.createDirectories(sandboxDir.resolve("outputs"))

    val operator = createOperator(opMeta, payload.params)

    val maybeSeed = if (opMeta.defn.unstable) Some(payload.seed) else None
    val ctx = new OpContext(maybeSeed, sandboxDir.resolve("outputs"))
    val profiler = new JvmProfiler

    // The actual operator is the only profiled section. The outcome is either an output object or an exception.
    logger.debug(s"Starting operator ${opMeta.defn.name} (sandbox in ${sandboxDir.toAbsolutePath})")
    val res = profiler.profile {
      try {
        Some(operator.execute(ctx))
      } catch {
        case NonFatal(e) =>
          logger.warn(s"Unexpected error while executing operator ${opMeta.defn.name}", e)
          None
      }
    }
    logger.debug(s"Completed operator ${opMeta.defn.name}")

    // We convert the outcome into an exit code, artifacts, metrics and possibly and error.
    val metrics = profiler.metrics
    val artifacts = res.toSeq.flatMap((out: Any) => extractArtifacts(opMeta, out.asInstanceOf[Product]))

    // Sandbox directory can now be deleted. Caveat: If there was a fatal error before, this line will never be
    // reached and it will not be deleted.
    // TODO: clean only files that are not referenced in artifacts.
    // logger.debug(s"Cleaned sandbox")

    OpResult(successful = res.nonEmpty, artifacts, metrics)
  }

  private def createOperator(opMeta: OpMeta, inputs: Seq[NamedValue]): ScalaOperator[_] = {
    val args = opMeta.defn.inputs.map { attr =>
      inputs.find(_.name == attr.name) match {
        case None =>
          attr.defaultValue match {
            case Some(defaultValue) =>
              // Optional arguments (i.e., with a default value) are never Option[_]'s.
              Values.decode(defaultValue, attr.dataType)
            case None =>
              if (!attr.isOptional) {
                throw new IllegalArgumentException(s"Missing required input ${attr.name}")
              }
              // An optional argument always accept None as value.
              None
          }
        case Some(NamedValue(_, v)) =>
          val normalizedValue = Values.as(v, attr.dataType)
            .getOrElse(throw new IllegalArgumentException(s"Invalid input type for ${attr.name}: ${DataTypes.stringify(v.dataType)}"))
          val value = Values.decode(normalizedValue, attr.dataType)
          if (attr.isOptional) Some(value) else value
      }
    }
    opMeta.opClass.newInstance(args).asInstanceOf[ScalaOperator[_]]
  }

  private def extractArtifacts(opMeta: OpMeta, out: Product): Seq[NamedValue] = {
    opMeta.defn.outputs.zipWithIndex.map { case (attr, idx) =>
      val rawValue = out.productElement(idx)
      // The exception should never be thrown, if everything else is in place. It seems indeed
      // extremely difficult for the client to return something over the wrong type, because it
      // is checked by the JVM (unless he does some dangerous type cast).
      // If this exception is thrown, it is likely there is an error somewhere else, while
      // inferring the various types.
      Values
        .encode(rawValue, attr.dataType)
        .map(value => NamedValue(attr.name, value))
        .getOrElse(throw new RuntimeException(s"Invalid output for ${attr.name}: $rawValue"))
    }
  }
}
