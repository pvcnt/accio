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

import fr.cnrs.liris.accio.sdk.{OpMeta, ScalaLibrary}

object OpsLibrary extends ScalaLibrary {
  override def ops: Seq[OpMeta] = Seq(
    OpMeta.apply[AreaCoverageOp],
    OpMeta.apply[CollapseTemporalGapsOp],
    OpMeta.apply[CountQueriesDistortionOp],
    OpMeta.apply[DataCompletenessOp],
    OpMeta.apply[DurationSplittingOp],
    OpMeta.apply[EnforceDurationOp],
    OpMeta.apply[EnforceSizeOp],
    OpMeta.apply[EventSourceOp],
    OpMeta.apply[GaussianKernelSmoothingOp],
    OpMeta.apply[GeoIndistinguishabilityOp],
    OpMeta.apply[HeatMapDistortionOp],
    //OpMeta.apply[MmcReidentOp],
    OpMeta.apply[ModuloSamplingOp],
    OpMeta.apply[PoisExtractionOp],
    OpMeta.apply[PoisReidentOp],
    OpMeta.apply[PoisRetrievalOp],
    OpMeta.apply[PromesseOp],
    OpMeta.apply[SequentialSplittingOp],
    OpMeta.apply[SizeSplittingOp],
    OpMeta.apply[SpatialDistortionOp],
    OpMeta.apply[SpatialGapSplittingOp],
    OpMeta.apply[SpatialSamplingOp],
    OpMeta.apply[SpatioTemporalDistortionOp],
    OpMeta.apply[TemporalGapSplittingOp],
    OpMeta.apply[TemporalSamplingOp],
    OpMeta.apply[UniformSamplingOp],
    OpMeta.apply[Wait4MeOp])
}
