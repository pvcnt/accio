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
 * Support for the [[http://crawdad.org/epfl/mobility/20090224/ Cabspotting dataset]].
 * Each trace is stored inside its own file, newest records first.
 */
case class CabspottingSource(url: String) extends DataSource[Trace] {
  override val decoder = new TraceDecoder(new CabspottingDecoder)
  override val reader = new WholeFileReader
  override val index = new CabspottingIndex(url)
}

object CabspottingSource {
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

  private[io] def extractUser(path: Path): String = path.getFileName.toString.drop(4).dropRight(4)
}

class CabspottingIndex(url: String) extends AbstractIndex {
  override protected def getFiles: Set[IndexedFile] =
    new File(url)
        .listFiles
        .filter(_.getName.startsWith("new_"))
        .map(_.toPath)
        .map(path => IndexedFile(path.toString, labels = Set(CabspottingSource.extractUser(path))))
        .toSet
}

class CabspottingDecoder extends Decoder[Record] {
  override def decode(record: EncodedRecord): Option[Record] = {
    val line = new String(record.bytes)
    val parts = line.trim.split(" ")
    if (parts.length < 4) {
      None
    } else {
      val lat = parts(0).toDouble
      val lng = parts(1).toDouble
      val time = new Instant(parts(3).toLong * 1000)
      val user = record.labels.mkString(",")
      try {
        Some(Record(user, LatLng.degrees(lat, lng).toPoint, time))
      } catch {
        //Error in original data, skip record.
        case e: IllegalArgumentException => None
      }
    }
  }
}
