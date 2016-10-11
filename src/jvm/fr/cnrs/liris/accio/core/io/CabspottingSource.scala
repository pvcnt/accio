package fr.cnrs.liris.accio.core.io

import java.nio.file.{Files, Path, Paths}

import fr.cnrs.liris.accio.core.dataset.{DataSource, Decoder, TextLineDecoder}
import fr.cnrs.liris.accio.core.model.{Event, Trace}
import fr.cnrs.liris.common.geo.LatLng
import org.joda.time.Instant

import scala.sys.process._

/**
 * Support for the [[http://crawdad.org/epfl/mobility/20090224/ Cabspotting dataset]].
 * Each trace is stored inside its own file, newest events first.
 */
case class CabspottingSource(url: String) extends DataSource[Trace] {
  private[this] val path = Paths.get(url)
  private[this] val decoder = new TextLineDecoder(new CabspottingDecoder)
  require(path.toFile.isDirectory, s"$url is not a directory")
  require(path.toFile.canRead, s"$url is unreadable")

  override lazy val keys = path.toFile
    .listFiles
    .filter(f => f.getName.startsWith("new_") && f.getName.endsWith(".txt"))
    .map(_.toPath.getFileName.toString.drop(4).dropRight(4))
    .toSeq
    .sorted

  override def read(key: String): Iterable[Trace] = {
    val events = decoder.decode(key, Files.readAllBytes(path.resolve(s"new_$key.txt"))).getOrElse(Seq.empty)
    if (events.nonEmpty) Iterable(Trace(events)) else Iterable.empty
  }
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
  override def decode(key: String, bytes: Array[Byte]): Option[Event] = {
    val line = new String(bytes)
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
        case e: IllegalArgumentException => None
      }
    }
  }
}