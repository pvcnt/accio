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
import fr.cnrs.liris.accio.domain.thrift.{OpPayload, ThriftAdapter}
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

import scala.util.control.NonFatal

trait ScalaLibrary extends Logging {
  def ops: Seq[OpMetadata]

  final def main(args: Array[String]): Unit = sys.exit(run(args))

  final def run(args: Array[String]): Int = {
    if (args.isEmpty) {
      printOperators()
    } else {
      Slf4jBridgeUtility.attemptSlf4jBridgeHandlerInstallation()
      // We technically allow more than 2 arguments, although there will be ignored.
      if (args.length < 2) {
        logger.error(s"At least arguments should be provided, got ${args.length}")
        2
      } else {
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
        return 6
    }
    operators.foreach { opMeta =>
      System.out.write(BinaryScroogeSerializer.toBytes(ThriftAdapter.toThrift(opMeta.defn)))
    }
    0
  }

  private def executeOperator(encodedPayload: String, outputFileName: String): Int = {
    // The payload is provided as the first argument. It is a based-64 encoded Thrift structure.
    val payload = try {
      ThriftAdapter.toDomain(BinaryScroogeSerializer.fromString(encodedPayload, OpPayload))
    } catch {
      case NonFatal(e) =>
        logger.error(s"Failed to read payload from $encodedPayload", e)
        return 5
    }
    val operators = try {
      ops
    } catch {
      case NonFatal(e) =>
        logger.error("Error while extracting operator definitions", e)
        return 6
    }
    operators.find(_.defn.name == payload.op) match {
      case None =>
        logger.error(s"Unknown operator: ${payload.op}")
        3
      case Some(opMeta) =>
        val executor = new OpExecutor(opMeta, Paths.get("."))
        val res = executor.execute(payload)
        val os = new FileOutputStream(outputFileName)
        try {
          BinaryScroogeSerializer.write(ThriftAdapter.toThrift(res), os)
        } catch {
          case NonFatal(e) =>
            logger.error(s"Failed to write result file to $outputFileName", e)
            return 4
        } finally {
          os.close()
        }
        if (res.successful) 0 else 1
    }
  }
}