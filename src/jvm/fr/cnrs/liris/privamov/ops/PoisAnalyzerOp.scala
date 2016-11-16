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

import com.github.nscala_time.time.Imports._
import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.common.geo.Distance
import fr.cnrs.liris.privamov.core.clustering.DTClusterer
import fr.cnrs.liris.privamov.core.model.{Poi, Trace}
import fr.cnrs.liris.privamov.core.sparkle.SparkleEnv

@Op(
  help = "Compute statistics about points of interest",
  description = "Compute statistics about the POIs that can be extracted from a trace, using a classical DT-clustering algorithm.",
  category = "metric")
class PoisAnalyzerOp @Inject()(env: SparkleEnv) extends Operator[PoisAnalyzerIn, PoisAnalyzerOut] with SparkleOperator {

  override def execute(in: PoisAnalyzerIn, ctx: OpContext): PoisAnalyzerOut = {
    val data = read(in.data, env)
    val clusterer = new DTClusterer(in.duration, in.diameter)
    val metrics = data.map { trace => evaluate(trace, clusterer) }.toArray
    PoisAnalyzerOut(
      count = metrics.map { case (k, v) => k -> v._1.toLong }.toMap,
      size = metrics.map { case (k, v) => k -> v._2.toLong }.toMap,
      duration = metrics.map { case (k, v) => k -> v._3.toLong }.toMap,
      sizeRatio = metrics.map { case (k, v) => k -> v._4 }.toMap,
      durationRatio = metrics.map { case (k, v) => k -> v._5 }.toMap)
  }

  private def evaluate(trace: Trace, clusterer: DTClusterer) = {
    val pois = clusterer.cluster(trace.events).map(c => Poi(c.events))
    val sizeInPoi = pois.map(_.size).sum
    val durationInPoi = pois.map(_.duration.seconds).sum
    trace.id -> (pois.size, sizeInPoi, durationInPoi, sizeInPoi.toDouble / trace.size, durationInPoi.toDouble / trace.duration.seconds)
  }
}

case class PoisAnalyzerIn(
  @Arg(help = "Clustering maximum diameter")
  diameter: Distance,
  @Arg(help = "Clustering minimum duration")
  duration: org.joda.time.Duration,
  @Arg(help = "Input dataset")
  data: Dataset)

case class PoisAnalyzerOut(
  @Arg(help = "POIs count")
  count: Map[String, Long],
  @Arg(help = "POIs size")
  size: Map[String, Long],
  @Arg(help = "POIs duration")
  duration: Map[String, Long],
  @Arg(help = "POIs size ratio")
  sizeRatio: Map[String, Double],
  @Arg(help = "POIs duration ratio")
  durationRatio: Map[String, Double])