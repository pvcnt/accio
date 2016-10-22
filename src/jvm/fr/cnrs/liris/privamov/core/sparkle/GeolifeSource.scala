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

package fr.cnrs.liris.privamov.core.sparkle

import java.nio.file.{Files, Path, Paths}

import fr.cnrs.liris.common.geo.LatLng
import fr.cnrs.liris.privamov.core.model.{Event, Trace}
import org.joda.time.Instant

import scala.sys.process._

/**
 * Support for the [[http://research.microsoft.com/apps/pubs/?id=152176 Geolife dataset]].
 * Each trace is stored inside its own directory splitted into multiple roughly one per day,
 * oldest events first.
 */
case class GeolifeSource(url: String) extends DataSource[Trace] {
  private[this] val path = Paths.get(url)
  private[this] val decoder = new TextLineDecoder(new GeolifeDecoder, headerLines = 6)
  require(path.toFile.isDirectory, s"$url is not a directory")
  require(path.toFile.canRead, s"$url is unreadable")

  override lazy val keys =
    path.toFile
      .listFiles
      .filter(_.isDirectory)
      .flatMap(_.toPath.resolve("Trajectory").toFile.listFiles)
      .map(_.toPath.getParent.getParent.getFileName.toString)
      .toSeq
      .sorted

  override def read(key: String): Iterable[Trace] = {
    val events = path.resolve("Trajectory").toFile.listFiles.sortBy(_.getName).flatMap(file => read(key, file.toPath))
    if (events.nonEmpty) Iterable(Trace(events)) else Iterable.empty
  }

  private def read(key: String, path: Path) =
    decoder.decode(key, Files.readAllBytes(path.resolve(s"$key.plt"))).getOrElse(Seq.empty)
}

/**
 * Factory for [[GeolifeSource]].
 */
object GeolifeSource {
  def download(dest: Path): GeolifeSource = {
    val zipFile = dest.resolve("geolife.zip").toFile
    var exitCode = (s"curl -L http://ftp.research.microsoft.com/downloads/b16d359d-d164-469e-9fd4-daa38f2b2e13/Geolife\\ Trajectories\\ 1.3.zip" #> zipFile).!
    if (0 != exitCode) {
      throw new RuntimeException(s"Error while fetching the Geolife archive: $exitCode")
    }
    exitCode = s"unzip -d ${dest.toAbsolutePath} ${zipFile.getAbsolutePath}".!
    if (0 != exitCode) {
      throw new RuntimeException(s"Error while extracting the Geolife archive: $exitCode")
    }
    zipFile.delete()
    new GeolifeSource(dest.toAbsolutePath.toString)
  }
}

class GeolifeDecoder extends Decoder[Event] {
  override def decode(key: String, bytes: Array[Byte]): Option[Event] = {
    val line = new String(bytes)
    val parts = line.trim.split(",")
    if (parts.length < 7) {
      None
    } else {
      val lat = parts(0).toDouble
      val lng = parts(1).toDouble
      val time = Instant.parse(s"${parts(5)}T${parts(6)}Z")
      try {
        Some(Event(key, LatLng.degrees(lat, lng).toPoint, time))
      } catch {
        //Error in original data, skip event.
        case e: IllegalArgumentException => None
      }
    }
  }
}
