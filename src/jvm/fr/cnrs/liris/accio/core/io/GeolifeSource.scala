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
import java.nio.file.{Path, Paths}

import fr.cnrs.liris.common.io.source._
import fr.cnrs.liris.common.geo.LatLng
import fr.cnrs.liris.accio.core.model.{Record, Trace}
import org.joda.time.Instant

import scala.reflect._
import scala.sys.process._

/**
 * Support for the [[http://research.microsoft.com/apps/pubs/?id=152176 Geolife dataset]].
 * Each trace is stored inside its own directory splitted into multiple roughly one per day,
 * oldest records first.
 */
case class GeolifeSource(url: String) extends DataSource[Trace] {
  override val decoder = new TraceDecoder(new GeolifeDecoder)
  override val reader = new TextLineReader(headerLines = 6) //TODO: fixme
  override val index = new GeolifeIndex(url)
}

object GeolifeSource {
  def download(dest: Path): GeolifeSource = {
    val zipFile = dest.resolve("geolife.zip").toFile
    var exitCode = (s"curl http://ftp.research.microsoft.com/downloads/b16d359d-d164-469e-9fd4-daa38f2b2e13/Geolife\\ Trajectories\\ 1.3.zip" #> zipFile).!
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

  private[io] def extractUser(path: Path): String = path.getParent.getParent.getFileName.toString
}

class GeolifeIndex(url: String) extends AbstractIndex {
  override protected def getFiles: Set[IndexedFile] =
    new File(url)
        .listFiles
        .filter(_.isDirectory)
        .flatMap(_.toPath.resolve("Trajectory").toFile.listFiles.map(_.toPath))
        .map(path => IndexedFile(path.toString, labels = Set(GeolifeSource.extractUser(path))))
        .toSet
}

class GeolifeDecoder extends Decoder[Record] {
  override def decode(ser: EncodedRecord): Option[Record] = {
    val line = new String(ser.bytes)
    val parts = line.trim.split(",")
    if (parts.length < 7) {
      None
    } else {
      val lat = parts(0).toDouble
      val lng = parts(1).toDouble
      val time = Instant.parse(s"${parts(5)}T${parts(6)}Z")
      val username = GeolifeSource.extractUser(Paths.get(ser.url))
      try {
        Some(Record(username, LatLng.degrees(lat, lng).toPoint, time))
      } catch {
        //Error in original data, skip record.
        case e: IllegalArgumentException => None
      }
    }
  }
}
