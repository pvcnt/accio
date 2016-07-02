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

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

import com.github.nscala_time.time.Imports._
import com.google.common.base.Charsets
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.dataset._
import fr.cnrs.liris.accio.core.model.{Record, Trace}
import fr.cnrs.liris.common.geo.Point
import org.joda.time.Instant

/**
 * Mobility traces source reading data from our custom CSV format, with one file per trace.
 *
 * @param url     Path to directory where to read
 * @param charset Charset to use
 */
case class CsvSource(url: String, charset: Charset = Charsets.UTF_8) extends DataSource[Trace] {
  private[this] val path = Paths.get(url)
  private[this] val decoder = new TextLineDecoder(new CsvDecoder(charset))

  override def keys: Seq[String] = path.toFile.listFiles.filter(_.isDirectory).map(_.toPath.getFileName.toString)

  override def read(key: String): Iterable[Trace] = {
    path.resolve(key)
      .toFile
      .listFiles
      .map(file => decoder.decode(key, Files.readAllBytes(file.toPath)))
      .flatMap {
        case Some(records) => Some(new Trace(key, records))
        case None => None
      }.toIterable
  }
}

/**
 * Mobility traces sink writing data to our custom CSV format, with one file per trace.
 *
 * @param url     Path to directory where to write
 * @param charset Charset to use
 */
case class CsvSink(url: String, charset: Charset = Charsets.UTF_8) extends DataSink[Trace] {
  private[this] val path = Paths.get(url)
  require(!path.toFile.exists || (path.toFile.isDirectory && path.toFile.listFiles.isEmpty), s"Non-empty directory: ${path.toAbsolutePath}")
  private[this] val encoder = new CsvEncoder(charset)
  private[this] val NL = "\n".getBytes(charset)

  override def write(key: String, elements: Iterator[Trace]): Unit = {
    Files.createDirectories(path.resolve(key))
    elements.zipWithIndex.foreach { case (trace, idx) =>
      val bytes = trace.records.map(encoder.encode).fold(Array.empty)(_ ++ NL ++ _)
      Files.write(path.resolve(key).resolve(s"$key-$idx.csv"), bytes)
    }
  }
}

/**
 * Decoder for our custo
 *
 * @param charset
 */
class CsvDecoder(charset: Charset = Charsets.UTF_8) extends Decoder[Record] with LazyLogging {
  override def decode(key: String, bytes: Array[Byte]): Option[Record] = {
    val line = new String(bytes, charset).trim
    if (line.isEmpty) {
      None
    } else {
      val parts = line.split(",")
      if (parts.length != 3) {
        logger.warn(s"Invalid line: $line")
        None
      } else {
        val x = parts(0).toDouble
        val y = parts(1).toDouble
        val time = new Instant(parts(2).toLong * 1000)
        Some(Record(key, Point(x, y), time))
      }
    }
  }
}

class CsvEncoder(charset: Charset = Charsets.UTF_8) extends Encoder[Record] {
  override def encode(obj: Record): Array[Byte] = {
    s"${obj.point.x},${obj.point.y},${obj.time.millis / 1000}".getBytes(charset)
  }
}
