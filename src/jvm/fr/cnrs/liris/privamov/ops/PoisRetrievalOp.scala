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

package fr.cnrs.liris.privamov.ops

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.common.geo.Distance
import fr.cnrs.liris.common.util.Requirements._
import fr.cnrs.liris.privamov.core.clustering.DTClusterer
import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.core.sparkle.SparkleEnv

@Op(
  category = "metric",
  help = "Compute POIs retrieval difference between two datasets of traces")
class PoisRetrievalOp @Inject()(env: SparkleEnv) extends Operator[PoisRetrievalIn, PoisRetrievalOut] with SparkleOperator {

  override def execute(in: PoisRetrievalIn, ctx: OpContext): PoisRetrievalOut = {
    val train = read(in.train, env)
    val test = read(in.test, env)
    val clusterer = new DTClusterer(in.duration, in.diameter)
    val metrics = train.zip(test).map { case (ref, res) => evaluate(ref, res, clusterer, in.threshold) }.toArray
    PoisRetrievalOut(
      precision = metrics.map { case (k, v) => k -> v._1 }.toMap,
      recall = metrics.map { case (k, v) => k -> v._2 }.toMap,
      fscore = metrics.map { case (k, v) => k -> v._3 }.toMap)
  }

  private def evaluate(ref: Trace, res: Trace, clusterer: DTClusterer, threshold: Distance) = {
    requireState(ref.id == res.id, s"Trace mismatch: ${ref.id} / ${res.id}")
    val refPois = clusterer.cluster(ref.events)
    val resPois = clusterer.cluster(res.events)
    val matched = resPois.flatMap { resPoi =>
      refPois.zipWithIndex.find { case (refPoi, _) =>
        refPoi.centroid.distance(resPoi.centroid) <= threshold
      }.map(_._2).toSeq
    }.toSet.size
    ref.id -> (MetricUtils.precision(resPois.size, matched), MetricUtils.recall(refPois.size, matched), MetricUtils.fscore(refPois.size, resPois.size, matched))
  }
}

case class PoisRetrievalIn(
  @Arg(help = "Clustering maximum diameter") diameter: Distance,
  @Arg(help = "Clustering minimum duration") duration: org.joda.time.Duration,
  @Arg(help = "Matching threshold") threshold: Distance,
  @Arg(help = "Train dataset") train: Dataset,
  @Arg(help = "Test dataset") test: Dataset)

case class PoisRetrievalOut(
  @Arg(help = "POIs retrieval precision") precision: Map[String, Double],
  @Arg(help = "POIs retrieval recall") recall: Map[String, Double],
  @Arg(help = "POIs retrieval F-Score") fscore: Map[String, Double])