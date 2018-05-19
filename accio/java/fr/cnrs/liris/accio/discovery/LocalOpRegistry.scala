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

package fr.cnrs.liris.accio.discovery

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.file.{Files, Path}

import com.google.common.io.ByteStreams
import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.domain.thrift.ThriftAdapter
import fr.cnrs.liris.accio.domain.{Operator, thrift}
import fr.cnrs.liris.util.jvm.JavaHome
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.control.NonFatal

final class LocalOpRegistry(directory: Path, filter: Option[String]) extends OpRegistry with Logging {
  override lazy val ops: Iterable[Operator] = {
    var it = Files.list(directory).iterator.asScala.filter(Files.isRegularFile(_))
    filter.foreach { filter =>
      val regex = filter.r
      it = it.filter(p => regex.findFirstIn(p.getFileName.toString).nonEmpty)
    }
    it.flatMap(readLibrary).toSet
  }

  private def readLibrary(file: Path): Set[Operator] = {
    try {
      val cmd = mutable.ListBuffer.empty[String]
      if (file.getFileName.endsWith(".jar")) {
        cmd += JavaHome.javaBinary.toString
        cmd ++= Seq("-jar", file.toAbsolutePath.toString)
      } else {
        cmd += file.toAbsolutePath.toString
      }

      val os = new ByteArrayOutputStream()
      val process = new ProcessBuilder()
        .command(cmd: _*)
        .redirectErrorStream(true)
        .start()
      ByteStreams.copy(process.getInputStream, os)
      process.waitFor()

      val is = new ByteArrayInputStream(os.toByteArray)
      val ops = mutable.Set.empty[Operator]
      while (is.available() > 0) {
        ops += ThriftAdapter.toDomain(BinaryScroogeSerializer.read(is, thrift.Operator))
      }
      ops.toSet
    } catch {
      case NonFatal(e) =>
        logger.warn(s"Error while reading library defined at $file", e)
        Set.empty
    }
  }
}
