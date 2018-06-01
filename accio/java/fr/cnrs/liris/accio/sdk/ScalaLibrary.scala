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

import java.io.FileOutputStream
import java.nio.file.Paths

import com.twitter.util.logging.{Logging, Slf4jBridgeUtility}
import fr.cnrs.liris.accio.domain.DataTypes
import fr.cnrs.liris.accio.domain.thrift.{OpPayload, ThriftAdapter}
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

import scala.util.control.NonFatal

/**
 * Trait used to help implementing an operator library in Scala. It is designed to be implenting
 * within an object, and provides the main class of the library's binary.
 */
trait ScalaLibrary extends Logging {

  import ScalaLibrary._

  /**
   * Return the metadata of the operators this library provides. This method may throw exceptions,
   * in which case they will be automatically caught later.
   */
  def ops: Seq[OpMetadata]

  /**
   * Library's entrypoint. Execute the given command and exits.
   *
   * @param args Command-line arguments.
   */
  final def main(args: Array[String]): Unit = sys.exit(run(args))

  /**
   * Execute the given command and exits. This is essentially a test-friendly version of [[main]],
   * as it does not brutally exits.
   *
   * @param args Command-line arguments.
   */
  final def run(args: Array[String]): Int = {
    DataTypes.register()

    if (args.isEmpty) {
      // If the binary is called without any argument, it is a request to print the Thrift-encoded
      // definition of the operators this library provides.
      printOperators()
    } else {
      // If arguments are provided, there should be two of them: the first one is the base 64
      // representation of a Thrift-encoded payload to execute, the second one is the path to a
      // file where to write the Thrift-encoded result of the execution.
      Slf4jBridgeUtility.attemptSlf4jBridgeHandlerInstallation()
      if (args.length < 2) {
        logger.error(s"At least arguments should be provided, got ${args.length}")
        CommandLineError
      } else {
        // We technically allow more than 2 arguments, although there will be ignored.
        executeOperator(args.head, args(1))
      }
    }
  }

  private def printOperators(): Int = {
    val operators = try {
      ops
    } catch {
      case NonFatal(e) =>
        logger.error("Error while extracting operator definitions", e)
        return InternalError
    }
    operators.foreach { opMeta =>
      System.out.write(BinaryScroogeSerializer.toBytes(ThriftAdapter.toThrift(opMeta.defn)))
    }
    Successful
  }

  private def executeOperator(encodedPayload: String, outputFileName: String): Int = {
    // The payload is provided as the first argument. It is a based-64 encoded Thrift structure.
    val payload = try {
      ThriftAdapter.toDomain(BinaryScroogeSerializer.fromString(encodedPayload, OpPayload))
    } catch {
      case NonFatal(e) =>
        logger.error(s"Failed to read payload from $encodedPayload", e)
        return CommandLineError
    }
    val operators = try {
      ops
    } catch {
      case NonFatal(e) =>
        logger.error("Error while extracting operator definitions", e)
        return InternalError
    }
    operators.find(_.defn.name == payload.op) match {
      case None =>
        logger.error(s"Unknown operator: ${payload.op}")
        CommandLineError
      case Some(opMeta) =>
        val executor = new OpExecutor(opMeta, Paths.get("."))
        val res = executor.execute(payload)
        val os = new FileOutputStream(outputFileName)
        try {
          BinaryScroogeSerializer.write(ThriftAdapter.toThrift(res), os)
        } catch {
          case NonFatal(e) =>
            logger.error(s"Failed to write result file to $outputFileName", e)
            return InternalError
        } finally {
          os.close()
        }
        if (res.successful) Successful else Failed
    }
  }
}

object ScalaLibrary {
  private[sdk] val Successful = 0
  private[sdk] val CommandLineError = 1
  private[sdk] val InternalError = 3
  private[sdk] val Failed = -1
}