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

import fr.cnrs.liris.accio.sdk.{OpMetadata, ScalaLibrary}

object OpsLibrary extends ScalaLibrary {
  override def ops: Seq[OpMetadata] = Seq(
    OpMetadata.apply[AreaCoverageOp],
    OpMetadata.apply[CollapseTemporalGapsOp],
    OpMetadata.apply[CountQueriesDistortionOp],
    OpMetadata.apply[DataCompletenessOp],
    OpMetadata.apply[DurationSplittingOp],
    OpMetadata.apply[EnforceDurationOp],
    OpMetadata.apply[EnforceSizeOp],
    OpMetadata.apply[EventSourceOp],
    OpMetadata.apply[GaussianKernelSmoothingOp],
    OpMetadata.apply[GeoIndistinguishabilityOp],
    OpMetadata.apply[HeatMapDistortionOp],
    //OpMeta.apply[MmcReidentOp],
    OpMetadata.apply[ModuloSamplingOp],
    OpMetadata.apply[PoisExtractionOp],
    OpMetadata.apply[PoisReidentOp],
    OpMetadata.apply[PoisRetrievalOp],
    OpMetadata.apply[PromesseOp],
    OpMetadata.apply[SequentialSplittingOp],
    OpMetadata.apply[SizeSplittingOp],
    OpMetadata.apply[SpatialDistortionOp],
    OpMetadata.apply[SpatialGapSplittingOp],
    OpMetadata.apply[SpatialSamplingOp],
    OpMetadata.apply[SpatioTemporalDistortionOp],
    OpMetadata.apply[TemporalGapSplittingOp],
    OpMetadata.apply[TemporalSamplingOp],
    OpMetadata.apply[UniformSamplingOp],
    OpMetadata.apply[Wait4MeOp])
}
