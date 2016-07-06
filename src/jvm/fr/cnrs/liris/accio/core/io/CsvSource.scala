/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.accio.core.io

import java.io.File
import java.nio.file.{Files, Paths}

import com.github.nscala_time.time.Imports._
import com.google.common.base.Charsets
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.dataset._
import fr.cnrs.liris.accio.core.model.{Event, Trace}
import fr.cnrs.liris.common.geo.Point
import org.joda.time.Instant

/**
 * Mobility traces source reading data from our custom CSV format, with one file per trace.
 *
 * @param url Path to directory where to read
 */
case class CsvSource(url: String) extends DataSource[Trace] {
  private[this] val path = Paths.get(url)
  private[this] val decoder = new TextLineDecoder(new CsvDecoder)
  require(path.toFile.isDirectory && path.toFile.canRead, s"Invalid or unreadable path $url")

  override def keys: Seq[String] = path.toFile.listFiles.filter(_.isDirectory).map(_.toPath.getFileName.toString)

  override def read(key: String): Iterable[Trace] = {
    path.resolve(key)
        .toFile
        .listFiles
        .toSeq
        .sortWith(sortFiles)
        .flatMap(read(key, _))
  }

  private def read(key: String, file: File): Option[Trace] = {
    decoder.decode(key, Files.readAllBytes(file.toPath)) match {
      case Some(events) => Some(new Trace(file.getName.dropRight(4), key, events))
      case None => None
    }
  }

  private def sortFiles(f1: File, f2: File): Boolean = {
    val parts1 = f1.getName.dropRight(4).split("-").tail.map(_.toInt)
    val parts2 = f2.getName.dropRight(4).split("-").tail.map(_.toInt)
    if (parts1.isEmpty) {
      true
    } else if (parts2.isEmpty) {
      false
    } else {
      for (i <- parts1.indices) {
        if (parts1(i) < parts2(i)) {
          return true
        } else if (parts1(i) > parts2(i)) {
          return false
        }
      }
      true
    }
  }
}

/**
 * Mobility traces sink writing data to our custom CSV format, with one file per trace.
 *
 * @param url Path to directory where to write
 */
case class CsvSink(url: String) extends DataSink[Trace] {
  private[this] val path = Paths.get(url)
  require(!path.toFile.exists || (path.toFile.isDirectory && path.toFile.listFiles.isEmpty),
    s"Non-empty directory: ${path.toAbsolutePath}")
  private[this] val encoder = new CsvEncoder
  private[this] val NL = "\n".getBytes(Charsets.UTF_8)

  override def write(key: String, elements: Iterator[Trace]): Unit = {
    Files.createDirectories(path.resolve(key))
    elements.foreach { trace =>
      val encodedEvents = trace.events.map(encoder.encode)
      val bytes = if (encodedEvents.isEmpty) {
        Array.empty[Byte]
      } else if (encodedEvents.size == 1) {
        encodedEvents.head
      } else {
        encodedEvents.tail.fold(encodedEvents.head)(_ ++ NL ++ _)
      }
      Files.write(path.resolve(key).resolve(s"${trace.id}.csv"), bytes)
    }
  }
}

/**
 * Decoder for our custom CSV format handling events.
 */
class CsvDecoder extends Decoder[Event] with LazyLogging {
  override def decode(key: String, bytes: Array[Byte]): Option[Event] = {
    val line = new String(bytes, Charsets.UTF_8).trim
    val parts = line.split(",")
    if (parts.length != 3) {
      logger.warn(s"Invalid line: $line")
      None
    } else {
      val x = parts(0).toDouble
      val y = parts(1).toDouble
      val time = new Instant(parts(2).toLong * 1000)
      Some(Event(key, Point(x, y), time))
    }
  }
}

/**
 * Encoder for our custom CSV format handling events.
 */
class CsvEncoder extends Encoder[Event] {
  override def encode(obj: Event): Array[Byte] = {
    s"${obj.point.x},${obj.point.y},${obj.time.millis / 1000}".getBytes(Charsets.UTF_8)
  }
}