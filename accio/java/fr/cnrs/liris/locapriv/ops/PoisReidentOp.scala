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

import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.accio.sdk._
import fr.cnrs.liris.locapriv.domain.{Poi, PoiSet}
import fr.cnrs.liris.sparkle.DataFrame

/**
 * Implementation of a re-identification attack using the points of interest as a discriminating
 * information. The POIs are used to model the behavior of training users, and then extracted from
 * the tracks of test users and compared to those from the training users. The comparison here is
 * done between set of POIs, and only the spatial information.
 *
 * Vincent Primault, Sonia Ben Mokhtar, Cédric Lauradoux and Lionel Brunie. Differentially Private
 * Location Privacy in Practice. In MOST'14.
 */
@Op(
  category = "metric",
  help = "Re-identification attack using POIs a the discriminating information.")
case class PoisReidentOp(
  @Arg(help = "Train dataset (POIs)")
  train: RemoteFile,
  @Arg(help = "Test dataset (POIs)")
  test: RemoteFile)
  extends ScalaOperator[PoisReidentOp.Out] with SparkleOperator {

  override def execute(ctx: OpContext): PoisReidentOp.Out = {
    val trainPois = readPois(train)
    val testPois = readPois(test)
    val metrics = computeMetrics(trainPois, testPois)
    val successRate = computeSuccessRate(trainPois, metrics)
    PoisReidentOp.Out(write(metrics, 0, ctx), successRate)
  }

  private def readPois(file: RemoteFile): Iterable[PoiSet] = {
    val pois = read[Poi](file).collect()
    pois.groupBy(_.user).map { case (id, elements) => PoiSet(id, elements) }
  }

  private def computeMetrics(trainPois: Iterable[PoiSet], testPois: Iterable[PoiSet]): DataFrame[PoisReidentOp.Value] = {
    if (testPois.isEmpty) {
      env.emptyDataFrame
    } else {
      val metrics = testPois.map { pois =>
        val distances = if (pois.nonEmpty) {
          // Compute the distance between the set of POIs from the test user and the models (from the training users).
          // This will give us an association between a training user and a distance. We only keep finite distances.
          trainPois
            .map(model => model.user -> model.distance(pois).meters)
            .filter { case (_, d) => !d.isInfinite }
            .toSeq
        } else {
          Seq.empty
        }
        distances.sortBy(_._2).headOption match {
          case None => PoisReidentOp.Value("-", pois.user, Double.NaN)
          case Some((trainUser, d)) => PoisReidentOp.Value(trainUser, pois.user, d)
        }
      }
      env.parallelize(metrics)
    }
  }

  private def computeSuccessRate(trainPois: Iterable[PoiSet], matches: DataFrame[PoisReidentOp.Value]) = {
    val trainUsers = trainPois.map(_.user)
    matches.map(row => if (row.correct) 1 else 0).sum / trainUsers.size.toDouble
  }
}

object PoisReidentOp {

  case class Value(trainUser: String, testUser: String, distance: Double) {
    def correct: Boolean = trainUser == testUser
  }

  case class Out(
    @Arg(help = "Metrics dataset")
    metrics: RemoteFile,
    @Arg(help = "Correct re-identifications rate")
    rate: Double)

}