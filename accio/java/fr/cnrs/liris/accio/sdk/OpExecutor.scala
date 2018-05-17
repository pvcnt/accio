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

package fr.cnrs.liris.accio.sdk

import java.nio.file.{Files, Path}
import java.util.UUID

import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.domain.{Attribute, OpPayload, OpResult, Operator}
import fr.cnrs.liris.lumos.domain.{AttrValue, ErrorDatum, Value}

import scala.util.control.NonFatal

/**
 * Entry point for executing an operator from a payload. It manages the whole lifecycle of
 * instantiating an operator, executing it, collecting its outputs and returning them.
 *
 * The main goal here is to enforce reproducibility of the execution. Given the same inputs, the
 * result should be the same outputs (modulo the metrics, as timing or load information may vary
 * and are not controllable).
 *
 * @param opDef   Operator definition.
 * @param clazz   Operator class.
 * @param workDir Path to the sandbox, where the executor can write temporary files.
 */
final class OpExecutor(opDef: Operator, clazz: Class[_], workDir: Path) extends Logging {
  /**
   * Execute an operator.
   *
   * @param payload Operator payload.
   * @return Result of the execution.
   */
  def execute(payload: OpPayload): OpResult = {
    try {
      val sandboxDir = workDir.resolve(UUID.randomUUID().toString)
      Files.createDirectories(sandboxDir.resolve("outputs"))

      val operator = createOperator(payload.params)

      val maybeSeed = if (opDef.unstable) Some(payload.seed) else None
      val ctx = new OpContext(maybeSeed, sandboxDir.resolve("outputs"))
      val profiler = new JvmProfiler

      // The actual operator is the only profiled section. The outcome is either an output object or an exception.
      logger.debug(s"Starting operator ${opDef.name} (sandbox in ${sandboxDir.toAbsolutePath})")
      val res = profiler.profile {
        try {
          Right(operator.execute(ctx))
        } catch {
          case NonFatal(e) =>
            logger.warn(s"Unexpected error while executing operator ${opDef.name}", e)
            Left(ErrorDatum.create(e))
        }
      }
      logger.debug(s"Completed operator ${opDef.name}")

      // We convert the outcome into an exit code, artifacts, metrics and possibly and error.
      val metrics = profiler.metrics
      val artifacts = res.right.toSeq.flatMap((out: Any) => extractArtifacts(out.asInstanceOf[Product]))
      val error = res.left.toOption

      // Sandbox directory can now be deleted. Caveat: If there was a fatal error before, this line
      // will never be reached and it will not be deleted.
      // TODO: clean only files that are not referenced in artifacts.
      // logger.debug(s"Cleaned sandbox")

      OpResult(successful = error.isEmpty, artifacts, metrics, error)
    } catch {
      case NonFatal(e) => OpResult(successful = false, error = Some(ErrorDatum.create(e)))
    }
  }

  private def createOperator(inputs: Seq[AttrValue]): ScalaOperator[_] = {
    val args = opDef.inputs.map { attr =>
      inputs.find(_.name == attr.name) match {
        case None =>
          attr.defaultValue match {
            case Some(defaultValue) =>
              // Attributes with a default value are never optional (there is hence no need to
              // wrap the value into an Option).
              decode(attr, defaultValue)
            case None =>
              // An optional argument always accept None as value.
              if (!attr.optional) {
                throw new IllegalArgumentException(s"Missing required input ${attr.name}")
              }
              None
          }
        case Some(value) =>
          // The attribute may be optional, which means the value has to wrapped inside an Option.
          val arg = decode(attr, value.value)
          if (attr.optional) Some(arg) else arg
      }
    }
    val ctor = clazz.getConstructors.head
    ctor.newInstance(args.map(_.asInstanceOf[AnyRef]): _*).asInstanceOf[ScalaOperator[_]]
  }

  private def extractArtifacts(out: Product): Seq[AttrValue] = {
    opDef.outputs.zipWithIndex.map { case (attr, idx) =>
      val rawValue = out.productElement(idx)
      // The exception should never be thrown, if everything else is correct. It seems indeed
      // extremely difficult for the client to return something over the wrong type, because it
      // is checked by the JVM (unless he does some dangerous type cast).
      // If this exception is thrown, it is likely there is an error somewhere else, while
      // inferring the various types.
      AttrValue(
        name = attr.name,
        dataType = attr.dataType,
        aspects = attr.aspects,
        value = encode(attr, rawValue))
    }
  }

  private def encode(attr: Attribute, v: Any): Value = {
    Values.encode(v, attr.dataType, attr.aspects).getOrElse {
      throw new RuntimeException(s"Invalid output type for ${attr.name}: ${v.getClass.getName}")
    }
  }

  private def decode(attr: Attribute, value: Value): Any = {
    Values.decode(value, attr.dataType, attr.aspects).getOrElse {
      throw new IllegalArgumentException(s"Invalid input type for ${attr.name}: ${value.dataType}")
    }
  }
}
