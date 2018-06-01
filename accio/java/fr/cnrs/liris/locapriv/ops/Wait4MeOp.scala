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

package fr.cnrs.liris.locapriv.ops

import java.io.{ByteArrayOutputStream, FileOutputStream}
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.{Files, Path}
import java.util.Locale

import com.google.common.io.{ByteStreams, Resources}
import fr.cnrs.liris.accio.sdk._
import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.sparkle.DataFrame
import fr.cnrs.liris.util.geo.{BoundingBox, Distance, Point}
import org.joda.time.{Duration, Instant}

import scala.util.control.NoStackTrace

/**
 * Wrapper around the implementation of the Wait4Me algorithm, as provided by their authors.
 *
 * Wait 4 Me: Time-tolerant Anonymization of Moving Objects Databases. Abul, Osman, Bonchi,
 * Francesco, Nanni, Mirco. Information Systems Journal, Volume 35, Issue 8, December 2010,
 * pp 884-910.
 *
 * @see http://www-kdd.isti.cnr.it/W4M/
 */
@Op(
  help = "Time-tolerant k-anonymization",
  description = "Wrapper around the implementation of the Wait4Me algorithm provided by their authors.",
  category = "lppm")
case class Wait4MeOp(
  @Arg(help = "Input dataset")
  data: RemoteFile,
  @Arg(help = "Anonymity level")
  k: Int,
  @Arg(help = "Uncertainty")
  delta: Distance,
  @Arg(help = "Initial maximum radius used in clustering")
  radiusMax: Option[Distance] = None,
  @Arg(help = "Global maximum trash size, in percentage of the dataset size")
  trashMax: Double = 0.1,
  @Arg(help = "Whether to chunk the input dataset")
  chunk: Boolean = false)
  extends ScalaOperator[Wait4MeOp.Out] with SparkleOperator {

  override def execute(ctx: OpContext): Wait4MeOp.Out = {
    val input = read[Event](data)
    if (input.count() == 0) {
      Wait4MeOp.Out(
        data = data,
        trashSize = 0,
        trashedPoints = 0,
        discernibility = 0,
        totalXyTranslations = Distance.Zero,
        totalTimeTranslations = Duration.ZERO,
        xyTranslationsCount = 0,
        timeTranslationsCount = 0,
        createdPoints = 0,
        deletedPoints = 0,
        meanSpatialTraceTranslation = Distance.Zero,
        meanTemporalTraceTranslation = Duration.ZERO,
        meanSpatialPointTranslation = Distance.Zero,
        meanTemporalPointTranslation = Duration.ZERO)
    } else {
      val localBinary = copyBinary(ctx.workDir, chunk)

      // Convert the trash max from a percentage to an absolute size.
      val users = input.groupBy(_.id).map(_._1).collect().sorted
      val trashMaxPercent = trashMax * users.length

      val radiusMaxOrDefault = radiusMax.getOrElse {
        // If not radiusMax is given, we initialize it to "0.5% of the semi-diagonal of the spatial
        // minimum bounding box of the dataset", as proposed by the authors of the paper.
        val boundingBox = getBoundingBox(input)
        boundingBox.diagonal / 2 * 0.5 / 100
      }

      // We convert our dataset to the format required by W4M.
      val w4mInputUri = ctx.workDir.resolve("w4mdata").toAbsolutePath.toString
      toW4MDataFrame(input, users)
        .write
        .option("delimiter", '\t')
        .option("header", false)
        .csv(w4mInputUri)

      val command = Seq(
        localBinary.toString,
        s"$w4mInputUri/0.csv",
        "out", /* output files prefix */
        k.toString,
        delta.meters.toString,
        radiusMaxOrDefault.meters.toString,
        trashMaxPercent.toString,
        "10" /* "if no idea enter 10" as suggested by paper's authors */)
      val os = new ByteArrayOutputStream()
      val process = new ProcessBuilder()
        .command(command: _*)
        .directory(ctx.workDir.toFile)
        .redirectErrorStream(true)
        .start()
      ByteStreams.copy(process.getInputStream, os)
      val exitValue = process.waitFor()

      if (exitValue != 0) {
        throw new RuntimeException("Error while executing Wait4Me binary, stdout/stderr follows.\n  " + os.toString) with NoStackTrace
      }

      // We convert back the result into a conventional dataset format.
      val w4mOutputPath = ctx.workDir.resolve(f"out_${k}_${"%.3f".formatLocal(Locale.ENGLISH, delta.meters)}.txt").toString
      val output = fromW4MDataFrame(env.read[Wait4MeOp.W4MEvent].option("delimiter", '\t').csv(w4mOutputPath), users)

      // We extract metrics from the captured stdout. This is quite ugly, but this works.
      // After header and progress information, result looks like:
      // ------------------------------------------
      // Some statistics
      // Number of trajs:1069
      // Number of trajectory points:59805
      // Pseudo diameter: 109554.43835
      // Trash_size:70
      // ...
      val resultLines = os.toString().split('\n')
      val statLines = resultLines.dropWhile(line => !line.startsWith("------")).drop(2)
      val metrics = statLines.map(_.split(":").last.trim)

      val d = write(output, 0, ctx)
      //println("total events " + env.read[Event].csv(d.uri).count())
      //Files.list(Paths.get(d.uri)).iterator.asScala.foreach(p => Files.copy(p, Paths.get(s"/Users/vincent/workspace/accio/${p.getFileName}")))

      Wait4MeOp.Out(
        data = d,
        trashSize = metrics(3).toInt,
        trashedPoints = metrics(4).toLong,
        discernibility = metrics(5).toLong,
        totalXyTranslations = Distance.meters(metrics(6).toDouble),
        totalTimeTranslations = Duration.millis(metrics(7).toDouble.round),
        xyTranslationsCount = metrics(8).toInt,
        timeTranslationsCount = metrics(9).toInt,
        createdPoints = metrics(10).toInt,
        deletedPoints = metrics(11).toInt,
        meanSpatialTraceTranslation = Distance.meters(metrics(12).toDouble),
        meanTemporalTraceTranslation = Duration.millis(metrics(13).toDouble.round),
        meanSpatialPointTranslation = Distance.meters(metrics(14).toDouble),
        meanTemporalPointTranslation = Duration.millis(metrics(15).toDouble.round))
    }
  }

  private def getPlatform = {
    val os = sys.props.getOrElse("os.name", "generic").toLowerCase(Locale.ENGLISH)
    if (os.contains("mac") || os.contains("darwin")) {
      "mac"
    } else if (os.contains("nux") || os.contains("nix") || os.contains("aix")) {
      "linux"
    } else {
      throw new RuntimeException(s"Unsupported platform: $os")
    }
  }

  private def limit(trace: Iterable[Event]): Iterable[Event] = {
    // Because of the binary code we use (provided by the authors), each trace is limited to 10000 events. We enforce
    // here this limit by sampling larger traces using the modulo operator.
    if (trace.size > Wait4MeOp.MaxTraceSize) {
      val modulo = trace.size.toDouble / Wait4MeOp.MaxTraceSize
      trace.zipWithIndex
        .filter { case (_, idx) => (idx % modulo) < 1 }
        .map(_._1)
        .take(Wait4MeOp.MaxTraceSize)
    } else {
      trace
    }
  }

  private def copyBinary(tmpDir: Path, chunk: Boolean) = {
    val jarBinary = s"fr/cnrs/liris/locapriv/ops/${getPlatform}_w4m_LST${if (chunk) "_chunk" else ""}"
    Files.createDirectories(tmpDir)
    val localBinary = tmpDir.resolve("program")
    val fos = new FileOutputStream(localBinary.toFile)
    try {
      Resources.copy(Resources.getResource(jarBinary), fos)
    } finally {
      fos.close()
    }
    localBinary.toFile.setExecutable(true)
    localBinary
  }

  private def fromW4MDataFrame(df: DataFrame[Wait4MeOp.W4MEvent], users: Array[String]): DataFrame[Event] = {
    val keysReverseIndex = users.zipWithIndex.map { case (k, v) => v -> k }.toMap
    //TODO: partition by user. Right now there is only a single partition.
    df.map { event =>
      Event(keysReverseIndex(event.id), Point(event.x, event.y), new Instant(event.time * 1000))
    }
  }

  private def toW4MDataFrame(df: DataFrame[Event], users: Array[String]): DataFrame[Wait4MeOp.W4MEvent] = {
    // Wait4Me requires events to be written in chronological order and grouped by user. This is
    // by default already the case with our CSV-based format, so we do not have more to handle
    // here.
    val keysIndex = users.zipWithIndex.toMap
    df.groupBy(_.id)
      .flatMap { case (id, trace) =>
        val numericId = keysIndex(id)
        limit(trace).map { event =>
          val point = event.point
          Wait4MeOp.W4MEvent(numericId, event.time.getMillis / 1000, point.x, point.y)
        }
      }.coalesce() // Force a single partition, and hence everything to be written inside a single file.
  }

  private def getBoundingBox(df: DataFrame[Event]): BoundingBox = {
    val minX = df.map(_.point.x).min
    val maxX = df.map(_.point.x).max
    val minY = df.map(_.point.y).min
    val maxY = df.map(_.point.y).max
    BoundingBox(Point(minX, minY), Point(maxX, maxY))
  }
}

object Wait4MeOp {
  private val MaxTraceSize = 10000

  private case class W4MEvent(id: Int, time: Long, x: Double, y: Double)

  case class Out(
    @Arg(help = "Output dataset")
    data: RemoteFile,
    @Arg(help = "Trash_size")
    trashSize: Int,
    @Arg(help = "Number of trashed points")
    trashedPoints: Long,
    @Arg(help = "Discernibility metric")
    discernibility: Long,
    @Arg(help = "Total XY translations")
    totalXyTranslations: Distance,
    @Arg(help = "Total time translations")
    totalTimeTranslations: Duration,
    @Arg(help = "XY translation count")
    xyTranslationsCount: Int,
    @Arg(help = "Time translation count")
    timeTranslationsCount: Int,
    @Arg(help = "Number of created points")
    createdPoints: Int,
    @Arg(help = "Number of deleted points")
    deletedPoints: Int,
    @Arg(help = "Mean spatial translation (per trace)")
    meanSpatialTraceTranslation: Distance,
    @Arg(help = "Mean temporal translation (per trace)")
    meanTemporalTraceTranslation: Duration,
    @Arg(help = "Mean spatial translation (per point)")
    meanSpatialPointTranslation: Distance,
    @Arg(help = "Mean temporal translation (per point)")
    meanTemporalPointTranslation: Duration)

}