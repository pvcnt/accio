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

package fr.cnrs.liris.locapriv.io

import java.nio.file.{Files, Path, Paths}

import com.google.common.base.MoreObjects
import fr.cnrs.liris.locapriv.domain.{Event, Trace}
import fr.cnrs.liris.sparkle.DataSource
import fr.cnrs.liris.util.geo.LatLng
import org.joda.time.Instant

import scala.reflect._
import scala.sys.process._

/**
 * Support for the [[http://crawdad.org/epfl/mobility/20090224/ Cabspotting dataset]].
 * Each trace is stored inside its own file, newest events first.
 *
 * @param uri Path to the directory from where to read.
 */
case class CabspottingSource(uri: String) extends DataSource[Trace] {
  private[this] val path = Paths.get(uri)
  private[this] val decoder = new TraceDecoder
  require(path.toFile.isDirectory, s"$uri is not a directory")
  require(path.toFile.canRead, s"$uri is unreadable")

  override lazy val keys: Seq[String] = path.toFile
    .listFiles
    .filter(f => f.getName.startsWith("new_") && f.getName.endsWith(".txt"))
    .map(_.toPath.getFileName.toString.drop(4).dropRight(4))
    .toSeq
    .sorted

  override def read(key: String): Seq[Trace] = {
    decoder.decode(key, Files.readAllBytes(path.resolve(s"new_$key.txt")))
  }

  override def toString: String =
    MoreObjects.toStringHelper(this)
      .add("uri", uri)
      .toString
}

/**
 * Factory for [[CabspottingSource]].
 */
object CabspottingSource {
  /**
   * Download the Cabspotting dataset from Crawdad. Credentials are needed.
   *
   * @param dest            Destination path
   * @param crawdadUsername Crawdad username
   * @param crawdadPassword Crawdad password
   * @return A Cabspotting source
   */
  def download(dest: Path, crawdadUsername: String, crawdadPassword: String): CabspottingSource = {
    val tarFile = dest.resolve("cabspotting.tar.gz").toFile
    var exitCode = (s"curl -u $crawdadUsername:$crawdadPassword -q http://uk.crawdad.org//download/epfl/mobility/cabspottingdata.tar.gz" #> tarFile).!
    if (0 != exitCode) {
      throw new RuntimeException(s"Error while fetching the Cabspotting archive: $exitCode")
    }
    exitCode = (tarFile #< s"tar xz -C ${dest.toAbsolutePath}").!
    if (0 != exitCode) {
      throw new RuntimeException(s"Error while extracting the Cabspotting archive: $exitCode")
    }
    tarFile.delete()
    new CabspottingSource(dest.toAbsolutePath.toString)
  }
}

/**
 * Decoder decoding a line of a Cabspotting file into an event.
 */
class CabspottingDecoder extends Decoder[Event] {
  override def elementClassTag: ClassTag[Event] = classTag[Event]

  override def decode(key: String, bytes: Array[Byte]): Seq[Event] = {
    new String(bytes)
      .split("\n")
      .toSeq
      .flatMap(line => decodeEvent(key, line.trim))
  }

  private def decodeEvent(key: String, line: String) = {
    val parts = line.trim.split(" ")
    if (parts.length < 4) {
      None
    } else {
      val lat = parts(0).toDouble
      val lng = parts(1).toDouble
      val time = new Instant(parts(3).toLong * 1000)
      try {
        Some(Event(key, LatLng.degrees(lat, lng).toPoint, time))
      } catch {
        //Error in original data, skip record.
        case _: IllegalArgumentException => None
      }
    }
  }
}