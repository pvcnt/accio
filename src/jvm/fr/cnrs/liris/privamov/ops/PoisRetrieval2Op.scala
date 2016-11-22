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

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.common.geo.Distance
import fr.cnrs.liris.common.util.Requirements._
import fr.cnrs.liris.privamov.core.io.Decoder
import fr.cnrs.liris.privamov.core.model.{Poi, PoiSet}
import fr.cnrs.liris.privamov.core.sparkle.SparkleEnv

@Op(
  category = "metric",
  help = "Compute POIs retrieval difference between two POIs datasets")
class PoisRetrieval2Op @Inject()(
  override protected val env: SparkleEnv,
  override protected val decoders: Set[Decoder[_]])
  extends Operator[PoisRetrieval2In, PoisRetrieval2Out] with SparkleReadOperator {

  override def execute(in: PoisRetrieval2In, ctx: OpContext): PoisRetrieval2Out = {
    val train = read[PoiSet](in.train)
    val test = read[PoiSet](in.test)
    val metrics = train.zip(test).map { case (ref, res) => evaluate(ref, res, in.threshold) }.toArray
    PoisRetrieval2Out(
      precision = metrics.map { case (k, v) => k -> v._1 }.toMap,
      recall = metrics.map { case (k, v) => k -> v._2 }.toMap,
      fscore = metrics.map { case (k, v) => k -> v._3 }.toMap)
  }

  private def evaluate(ref: PoiSet, res: PoiSet, threshold: Distance) = {
    requireState(ref.id == res.id, s"Trace mismatch: ${ref.id} / ${res.id}")
    val matched = res.pois.flatMap(resPoi => remap(resPoi, ref.pois, threshold)).distinct.size
    ref.id -> (MetricUtils.precision(res.size, matched), MetricUtils.recall(ref.size, matched), MetricUtils.fscore(ref.size, res.size, matched))
  }

  private def remap(resPoi: Poi, refPois: Seq[Poi], threshold: Distance) = {
    val matchingPois = refPois.zipWithIndex
      .map { case (refPoi, idx) => (idx, refPoi.centroid.distance(resPoi.centroid)) }
      .filter { case (_, d) => d <= threshold }
    if (matchingPois.nonEmpty) Some(matchingPois.minBy(_._2)._1) else None
  }
}

case class PoisRetrieval2In(
  @Arg(help = "Matching threshold") threshold: Distance,
  @Arg(help = "Train dataset") train: Dataset,
  @Arg(help = "Test dataset") test: Dataset)

case class PoisRetrieval2Out(
  @Arg(help = "POIs retrieval precision") precision: Map[String, Double],
  @Arg(help = "POIs retrieval recall") recall: Map[String, Double],
  @Arg(help = "POIs retrieval F-Score") fscore: Map[String, Double])