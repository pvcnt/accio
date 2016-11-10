/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.privamov.ops

import java.io.FileOutputStream
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.util.Locale

import com.google.common.io.Resources
import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.common.geo.{Distance, Point}
import fr.cnrs.liris.privamov.core.io.{CsvSink, DataSink}
import fr.cnrs.liris.privamov.core.model.{Event, Trace}
import fr.cnrs.liris.privamov.core.sparkle.SparkleEnv
import org.joda.time.{Duration, Instant}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Wrapper around the implementation of the Wait4Me algorithm, as provided by their authors.
 *
 * Wait 4 Me: Time-tolerant Anonymization of Moving Objects Databases. Abul, Osman, Bonchi, Francesco, Nanni, Mirco.
 * Information Systems Journal, Volume 35, Issue 8, December 2010, pp 884-910.
 *
 * @see http://www-kdd.isti.cnr.it/W4M/
 */
@Op(
  help = "Time-tolerant k-anonymization",
  category = "lppm")
class Wait4MeOp @Inject()(env: SparkleEnv) extends Operator[Wait4MeIn, Wait4MeOut] with SparkleOperator {

  override def execute(in: Wait4MeIn, ctx: OpContext): Wait4MeOut = {
    val input = read(in.data, env)
    if (input.count() == 0) {
      Wait4MeOut(
        data = in.data,
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
      val tmpDir = Files.createTempDirectory("accio-w4m-")
      val localBinary = copyBinary(tmpDir, in.chunk)

      // Convert the trash max from a percentage to an absolute size.
      val trashMax = in.trashMax * input.keys.size

      val radiusMax = in.radiusMax.getOrElse {
        // If not radiusMax is given, we initialize it to "0.5% of the semi-diagonal of the spatial minimum bounding
        // box of the dataset", as proposed by the authors of the paper.
        val boundingBox = input.map(_.boundingBox).reduce(_ union _)
        boundingBox.diagonal / 2 * 0.5 / 100
      }

      // We convert our dataset to the format required by W4M.
      val w4mInputUri = tmpDir.resolve("data.txt").toAbsolutePath.toString
      input.write(new W4MSink(w4mInputUri, input.keys.zipWithIndex.toMap))

      val process = new ProcessBuilder(localBinary.toAbsolutePath.toString, w4mInputUri, "out", in.k.toString, in.delta.meters.toString, radiusMax.meters.toString, trashMax.toString, "10")
        .directory(tmpDir.toFile)
        .redirectErrorStream(true)
        .redirectOutput(ProcessBuilder.Redirect.to(tmpDir.resolve("stdout").toFile))
        .start()
      val exitValue = process.waitFor()
      val resultLines = Files.readAllLines(tmpDir.resolve("stdout")).asScala

      if (exitValue != 0) {
        throw new RuntimeException("Error while executing Wait4Me binary, stdout/stderr follows.\n  " + resultLines.mkString("\n  "))
      }

      // We convert back the result into a conventional dataset format.
      val w4mOutputPath = tmpDir.resolve(f"out_${in.k}_${"%.3f".formatLocal(Locale.ENGLISH, in.delta.meters)}.txt").toAbsolutePath
      val output = writeOutput(input.keys , w4mOutputPath, ctx.workDir)

      // We extract metrics from the captured stdout. This is quite ugly, but this works.
      // After header and progress information, result looks like:
      // ------------------------------------------
      // Some statistics
      // Number of trajs:1069
      // Number of trajectory points:59805
      // Pseudo diameter: 109554.43835
      // Trash_size:70
      // ...
      val statLines = resultLines.dropWhile(line => !line.startsWith("------")).drop(2)
      val metrics = statLines.map(_.split(":").last.trim)

      Wait4MeOut(
        data = output,
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

  private def copyBinary(tmpDir: Path, chunk: Boolean) = {
    val jarBinary = s"fr/cnrs/liris/privamov/ops/${getPlatform}_w4m_LST${if (chunk) "_chunk" else ""}"
    val localBinary = tmpDir.resolve("program")
    Resources.copy(Resources.getResource(jarBinary), new FileOutputStream(localBinary.toFile))
    localBinary.toFile.setExecutable(true)
    localBinary
  }

  private def writeOutput(keys: Seq[String], w4mOutputPath: Path, workDir: Path) = {
    val keysReverseIndex = keys.zipWithIndex.map { case (k, v) => v -> k }.toMap
    var currIdx: Option[Int] = None
    val events = mutable.ListBuffer.empty[Event]
    val outputUri = workDir.resolve("data").toAbsolutePath.toString
    val sink = CsvSink(outputUri, failOnNonEmptyDirectory = false)
    Files.readAllLines(w4mOutputPath).asScala.foreach { line =>
      val parts = line.trim.split("\t")
      val idx = parts(0).toInt
      if (events.nonEmpty && currIdx.get != idx) {
        env.parallelize(keysReverseIndex(currIdx.get) -> Seq(Trace(keysReverseIndex(currIdx.get), events.toList))).write(sink)
        events.clear()
        currIdx = Some(idx)
      } else if (currIdx.isEmpty) {
        currIdx = Some(idx)
      }
      events += Event(keysReverseIndex(idx).split("-").head, Point(parts(2).toDouble, parts(3).toDouble), new Instant(parts(1).toLong))
    }
    if (events.nonEmpty) {
      env.parallelize(keysReverseIndex(currIdx.get) -> Seq(Trace(keysReverseIndex(currIdx.get), events.toList))).write(sink)
    }
    Dataset(outputUri, format = "csv")
  }
}

case class Wait4MeIn(
  @Arg(help = "Input dataset") data: Dataset,
  @Arg(help = "Anonymity level") k: Int,
  @Arg(help = "Uncertainty") delta: Distance,
  @Arg(help = "Initial maximum radius used in clustering") radiusMax: Option[Distance],
  @Arg(help = "Global maximum trash size, in percentage of the dataset size") trashMax: Double = 0.1,
  @Arg(help = "Whether to chunk the input dataset") chunk: Boolean = false)

case class Wait4MeOut(
  @Arg(help = "Output dataset") data: Dataset,
  @Arg(help = "Trash_size") trashSize: Int,
  @Arg(help = "Number of trashed points") trashedPoints: Long,
  @Arg(help = "Discernibility metric") discernibility: Long,
  @Arg(help = "Total XY translations") totalXyTranslations: Distance,
  @Arg(help = "Total time translations") totalTimeTranslations: Duration,
  @Arg(help = "XY translation count") xyTranslationsCount: Int,
  @Arg(help = "Time translation count") timeTranslationsCount: Int,
  @Arg(help = "Number of created points") createdPoints: Int,
  @Arg(help = "Number of deleted points") deletedPoints: Int,
  @Arg(help = "Mean spatial translation (per trace)") meanSpatialTraceTranslation: Distance,
  @Arg(help = "Mean temporal translation (per trace)") meanTemporalTraceTranslation: Duration,
  @Arg(help = "Mean spatial translation (per point)") meanSpatialPointTranslation: Distance,
  @Arg(help = "Mean temporal translation (per point)") meanTemporalPointTranslation: Duration)

private class W4MSink(uri: String, keysIndex: Map[String, Int]) extends DataSink[Trace] {
  val path = Paths.get(uri)
  Files.createDirectories(path.getParent)

  override def write(elements: TraversableOnce[Trace]): Unit = synchronized {
    val lines = elements.flatMap { trace =>
      trace.events.map { event =>
        s"${keysIndex(trace.id)}\t${event.time.getMillis / 1000}\t${event.point.x}\t${event.point.y}"
      }
    }.toSeq
    Files.write(path, lines.asJava, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
  }
}