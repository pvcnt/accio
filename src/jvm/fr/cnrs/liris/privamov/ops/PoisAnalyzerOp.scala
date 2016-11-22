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

import breeze.stats.mean
import com.github.nscala_time.time.Imports._
import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.common.geo.{Distance, Point}
import fr.cnrs.liris.privamov.core.io.{Decoder, Encoder}
import fr.cnrs.liris.privamov.core.model.{Poi, PoiSet}
import fr.cnrs.liris.privamov.core.sparkle.SparkleEnv

import scala.collection.mutable

@Op(
  help = "Compute statistics about points of interest",
  category = "metric")
class PoisAnalyzerOp @Inject()(
  override protected val env: SparkleEnv,
  override protected val decoders: Set[Decoder[_]],
  override protected val encoders: Set[Encoder[_]])
  extends Operator[PoisAnalyzerIn, PoisAnalyzerOut] with SparkleOperator {

  override def execute(in: PoisAnalyzerIn, ctx: OpContext): PoisAnalyzerOut = {
    val trainPois = read[PoiSet](in.train).flatMap(_.pois).toArray

    val pois = mutable.ListBuffer.empty[AggregatedPoi]
    trainPois.foreach { trainPoi =>
      pois.find(p => p.centroid.distance(trainPoi.centroid) <= in.threshold) match {
        case Some(matchingPoi) => matchingPoi.trainPois ++= Seq(trainPoi)
        case None => pois += new AggregatedPoi(trainPoi.centroid, Seq(trainPoi), Seq.empty)
      }
    }

    in.test.foreach { test =>
      val testPois = read[PoiSet](test).flatMap(_.pois)
      testPois.foreach { testPoi =>
        pois.find(p => p.centroid.distance(testPoi.centroid) <= in.threshold).foreach { matchingPoi =>
          matchingPoi synchronized {
            matchingPoi.testPois ++= Seq(testPoi)
          }
        }
      }
    }

    val header = Seq("poi_id", "avg_duration_in_millis", "nb_visits", "nb_users", "avg_size", "retrieved").mkString(",")
    val lines = pois.zipWithIndex.map { case (poi, idx) =>
      val fields = Seq(idx, poi.avgDuration.millis, poi.nbVisits, poi.nbUsers, poi.avgSize, if (poi.retrieved) "1" else "0")
      fields.map(_.toString).mkString(",")
    }
    val output = write(Seq(header) ++ lines, "pois", ctx.workDir)

    PoisAnalyzerOut(output)
  }
}

private class AggregatedPoi(val centroid: Point, var trainPois: Seq[Poi], var testPois: Seq[Poi]) {
  def retrieved: Boolean = testPois.exists(testPoi => trainPois.exists(_.user == testPoi.user))

  def avgDuration: Duration = Duration.millis(mean(trainPois.map(_.duration.getMillis.toDouble)).round)

  def avgSize: Double = mean(trainPois.map(_.size.toDouble))

  def nbVisits: Int = trainPois.size

  def nbUsers: Int = trainPois.map(_.user).distinct.size
}

case class PoisAnalyzerIn(
  @Arg(help = "Matching threshold")
  threshold: Distance,
  @Arg(help = "Train POIs dataset")
  train: Dataset,
  @Arg(help = "Test POIs dataset, to be compared with train dataset")
  test: Option[Dataset])

case class PoisAnalyzerOut(
  @Arg(help = "POIs analysis dataset")
  data: Dataset)