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
    ops.addBinding.toInstance(OpMeta.apply[AreaCoverageOp])
    ops.addBinding.toInstance(OpMeta.apply[CollapseTemporalGapsOp])
    ops.addBinding.toInstance(OpMeta.apply[CountQueriesDistortionOp])
    ops.addBinding.toInstance(OpMeta.apply[DataCompletenessOp])
    ops.addBinding.toInstance(OpMeta.apply[DurationSplittingOp])
    ops.addBinding.toInstance(OpMeta.apply[EnforceDurationOp])
    ops.addBinding.toInstance(OpMeta.apply[EnforceSizeOp])
    ops.addBinding.toInstance(OpMeta.apply[EventSourceOp])
    ops.addBinding.toInstance(OpMeta.apply[GaussianKernelSmoothingOp])
    ops.addBinding.toInstance(OpMeta.apply[GeoIndistinguishabilityOp])
    ops.addBinding.toInstance(OpMeta.apply[HeatMapDistortionOp])
    ops.addBinding.toInstance(OpMeta.apply[MmcReidentOp])
    ops.addBinding.toInstance(OpMeta.apply[ModuloSamplingOp])
    ops.addBinding.toInstance(OpMeta.apply[PoisExtractionOp])
    ops.addBinding.toInstance(OpMeta.apply[PoisReidentOp])
    ops.addBinding.toInstance(OpMeta.apply[PoisRetrievalOp])
    ops.addBinding.toInstance(OpMeta.apply[PromesseOp])
    ops.addBinding.toInstance(OpMeta.apply[SequentialSplittingOp])
    ops.addBinding.toInstance(OpMeta.apply[SizeSplittingOp])
    ops.addBinding.toInstance(OpMeta.apply[SpatialDistortionOp])
    ops.addBinding.toInstance(OpMeta.apply[SpatialGapSplittingOp])
    ops.addBinding.toInstance(OpMeta.apply[SpatialSamplingOp])
    ops.addBinding.toInstance(OpMeta.apply[SpatioTemporalDistortionOp])
    ops.addBinding.toInstance(OpMeta.apply[TemporalGapSplittingOp])
    ops.addBinding.toInstance(OpMeta.apply[TemporalSamplingOp])
    ops.addBinding.toInstance(OpMeta.apply[TransmissionDelayOp])
    ops.addBinding.toInstance(OpMeta.apply[UniformSamplingOp])
    ops.addBinding.toInstance(OpMeta.apply[Wait4MeOp])
  }
}
