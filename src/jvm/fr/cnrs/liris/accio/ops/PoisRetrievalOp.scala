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

package fr.cnrs.liris.accio.ops

import fr.cnrs.liris.accio.core.dataset.DataFrame
import fr.cnrs.liris.accio.core.framework.{Param, _}
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.common.util.Distance
import fr.cnrs.liris.common.util.Requirements._
import fr.cnrs.liris.privamov.clustering.DTClusterer

@Op(
  category = "metric",
  help = "Compute POIs retrieval difference between two datasets of traces")
case class PoisRetrievalOp(
  @Param(help = "Clustering maximum diameter")
  diameter: Distance,
  @Param(help = "Clustering minimum duration")
  duration: org.joda.time.Duration,
  @Param(help = "Matching threshold")
  threshold: Distance
) extends Evaluator[PoisRetrievalOp.Input, PoisRetrievalOp.Output] {


  override def execute(in: PoisRetrievalOp.Input, ctx: OpContext): PoisRetrievalOp.Output = {
    val metrics = in.train.zip(in.test).map { case (ref, res) =>
      requireState(ref.id == res.id, s"Trace mismatch: ${ref.id} / ${res.id}")
      val refPois = clusterer.cluster(ref.events)
      val resPois = clusterer.cluster(res.events)
      val matched = resPois.flatMap { resPoi =>
        refPois.zipWithIndex.find { case (refPoi, _) =>
          refPoi.centroid.distance(resPoi.centroid) <= threshold
        }.map(_._2).toSeq
      }.toSet.size
      (MetricUtils.precision(resPois.size, matched), MetricUtils.recall(refPois.size, matched), MetricUtils.fscore(refPois.size, resPois.size, matched))
    }.toArray

    PoisRetrievalOp.Output(
      precision = metrics.map(_._1),
      recall = metrics.map(_._2),
      fscore = metrics.map(_._3))
  }

  private[this] val clusterer = new DTClusterer(duration, diameter)

  override def evaluate(reference: Trace, result: Trace): Seq[Metric] = {
    val refPois = clusterer.cluster(reference.events)
    val resPois = clusterer.cluster(result.events)
    val matched = resPois.flatMap { resPoi =>
      refPois.zipWithIndex.find { case (refPoi, _) =>
        refPoi.centroid.distance(resPoi.centroid) <= threshold
      }.map(_._2).toSeq
    }.toSet.size
    MetricUtils.informationRetrieval(refPois.size, resPois.size, matched)
  }
}

object PoisRetrievalOp {

  case class Input(
    @In(help = "Train dataset") train: DataFrame[Trace],
    @In(help = "Test dataset") test: DataFrame[Trace])

  case class Output(
    @Out(help = "POIs retrieval precision") precision: Array[Double],
    @Out(help = "POIs retrieval recall") recall: Array[Double],
    @Out(help = "POIs retrieval F-Score") fscore: Array[Double])

}