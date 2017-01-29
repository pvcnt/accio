/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.common.geo.Distance
import fr.cnrs.liris.privamov.core.clustering.DTClusterer
import fr.cnrs.liris.privamov.core.model.{Poi, PoiSet, Trace}

@Op(
  category = "transform",
  help = "Compute POIs retrieval difference between two datasets of traces",
  cpu = 6,
  ram = "3G")
class PoisExtractionOp extends Operator[PoisExtractionIn, PoisExtractionOut] {

  override def execute(in: PoisExtractionIn, ctx: OpContext): PoisExtractionOut = {
    val input = ctx.read[Trace](in.data)
    val clusterer = new DTClusterer(in.duration, in.diameter)
    val output = input.map { trace =>
      val pois = clusterer.cluster(trace).map(cluster => Poi(cluster.events))
      PoiSet(trace.id, pois)
    }
    PoisExtractionOut(ctx.write(output))
  }
}

case class PoisExtractionIn(
  @Arg(help = "Clustering maximum diameter") diameter: Distance,
  @Arg(help = "Clustering minimum duration") duration: org.joda.time.Duration,
  @Arg(help = "Input traces dataset") data: Dataset)

case class PoisExtractionOut(
  @Arg(help = "Output POIs dataset") data: Dataset)