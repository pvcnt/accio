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

package fr.cnrs.liris.locapriv.install

import com.twitter.inject.TwitterModule
import fr.cnrs.liris.accio.runtime.OpMeta
import fr.cnrs.liris.locapriv.ops._
import net.codingwell.scalaguice.ScalaMultibinder

object OpsModule extends TwitterModule {
  override def configure(): Unit = {
    val ops = ScalaMultibinder.newSetBinder[OpMeta](binder)
    ops.addBinding.toInstance(OpMeta[AreaCoverageOp])
    ops.addBinding.toInstance(OpMeta[CollapseTemporalGapsOp])
    ops.addBinding.toInstance(OpMeta[CountQueriesDistortionOp])
    ops.addBinding.toInstance(OpMeta[DataCompletenessOp])
    ops.addBinding.toInstance(OpMeta[DurationSplittingOp])
    ops.addBinding.toInstance(OpMeta[EnforceDurationOp])
    ops.addBinding.toInstance(OpMeta[EnforceSizeOp])
    ops.addBinding.toInstance(OpMeta[EventSourceOp])
    ops.addBinding.toInstance(OpMeta[GaussianKernelSmoothingOp])
    ops.addBinding.toInstance(OpMeta[GeoIndistinguishabilityOp])
    ops.addBinding.toInstance(OpMeta[HeatMapDistortionOp])
    ops.addBinding.toInstance(OpMeta[MmcReidentOp])
    ops.addBinding.toInstance(OpMeta[ModuloSamplingOp])
    ops.addBinding.toInstance(OpMeta[PoisExtractionOp])
    ops.addBinding.toInstance(OpMeta[PoisReidentOp])
    ops.addBinding.toInstance(OpMeta[PoisRetrievalOp])
    ops.addBinding.toInstance(OpMeta[PromesseOp])
    ops.addBinding.toInstance(OpMeta[SequentialSplittingOp])
    ops.addBinding.toInstance(OpMeta[SizeSplittingOp])
    ops.addBinding.toInstance(OpMeta[SpatialDistortionOp])
    ops.addBinding.toInstance(OpMeta[SpatialGapSplittingOp])
    ops.addBinding.toInstance(OpMeta[SpatialSamplingOp])
    ops.addBinding.toInstance(OpMeta[SpatioTemporalDistortionOp])
    ops.addBinding.toInstance(OpMeta[TemporalGapSplittingOp])
    ops.addBinding.toInstance(OpMeta[TemporalSamplingOp])
    ops.addBinding.toInstance(OpMeta[TransmissionDelayOp])
    ops.addBinding.toInstance(OpMeta[UniformSamplingOp])
    ops.addBinding.toInstance(OpMeta[Wait4MeOp])
  }
}
