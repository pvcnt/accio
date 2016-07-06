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

package fr.cnrs.liris.accio.core.ops.eval

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.core.dataset.Dataset
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.core.model.{Poi, Trace}
import fr.cnrs.liris.accio.core.ops.eval.PoisAnalyzerOp.{Input, Output}
import fr.cnrs.liris.accio.core.param.Param
import fr.cnrs.liris.common.util.Distance
import fr.cnrs.liris.privamov.lib.clustering.DTClusterer

/**
 * Analyzer computing statistics about the POIs that can be extracted from a trace, using a
 * classical DJ-clustering algorithm.
 */
@Op(
  help = "Compute statistics about points of interest",
  category = "metric",
  metrics = Array("count", "size", "duration", "size_ratio", "duration_ratio")
)
case class PoisAnalyzerOp(
    @Param(help = "Clustering maximum diameter")
    diameter: Distance,
    @Param(help = "Clustering minimum duration")
    duration: Duration
) extends Analyzer[Input, Output] {
  private[this] val clusterer = new DTClusterer(duration, diameter)

  override def analyze(trace: Trace): Seq[Metric] = {
    val pois = clusterer.cluster(trace.events).map(c => Poi(c.events))
    val sizeInPoi = pois.map(_.size).sum
    val durationInPoi = pois.map(_.duration.seconds).sum
    Seq(
      Metric("count", pois.size),
      Metric("size", sizeInPoi),
      Metric("duration", durationInPoi),
      Metric("size_ratio", sizeInPoi.toDouble / trace.size),
      Metric("duration_ratio", durationInPoi.toDouble / trace.duration.seconds))
  }

  private def durationInPoi(data: Dataset[Poi]) = data.map(_.duration.seconds).reduce(_ + _)

  private def sizeInPoi(data: Dataset[Poi]) = data.map(_.size).reduce(_ + _)

  private def cluster(trace :Trace): Seq[Poi] =
    clusterer.cluster(trace.events).map(c => Poi(c.events))
}

object PoisAnalyzerOp {

  case class Input(@In(help = "Input dataset") data: Dataset[Trace])

  case class Output(
      @Out(help = "POIs counts") count: Dataset[Long],
      @Out(help = "POIs sizes") size: Dataset[Long],
      @Out(help = "POIs durations") duration: Dataset[Long],
      @Out(help = "POIs size ratios") sizeRatio: Dataset[Double],
      @Out(help = "POIs duration ratios") durationRatio: Dataset[Double]
  )

}