package fr.cnrs.liris.privamov.ops

import com.google.inject.TypeLiteral
import fr.cnrs.liris.accio.core.api.Operator
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}

object OpsModule extends ScalaModule {
  override def configure(): Unit = {
    val ops = ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Class[_ <: Operator[_, _]]] {})
    ops.addBinding.toInstance(classOf[AreaCoverageOp])
    ops.addBinding.toInstance(classOf[BasicAnalyzerOp])
    ops.addBinding.toInstance(classOf[CollapseTemporalGapsOp])
    ops.addBinding.toInstance(classOf[CountQueriesDistortionOp])
    ops.addBinding.toInstance(classOf[DataCompletenessOp])
    ops.addBinding.toInstance(classOf[DurationSplittingOp])
    ops.addBinding.toInstance(classOf[EnforceDurationOp])
    ops.addBinding.toInstance(classOf[EnforceSizeOp])
    ops.addBinding.toInstance(classOf[EventSourceOp])
    ops.addBinding.toInstance(classOf[GaussianKernelSmoothingOp])
    ops.addBinding.toInstance(classOf[GeoIndistinguishabilityOp])
    ops.addBinding.toInstance(classOf[SizeSplittingOp])
    ops.addBinding.toInstance(classOf[SpatialGapSplittingOp])
    ops.addBinding.toInstance(classOf[PoisAnalyzerOp])
    ops.addBinding.toInstance(classOf[PoisRetrievalOp])
    ops.addBinding.toInstance(classOf[PromesseOp])
    ops.addBinding.toInstance(classOf[PoisReidentOp])
    ops.addBinding.toInstance(classOf[SequentialSplittingOp])
    ops.addBinding.toInstance(classOf[SpatialSamplingOp])
    ops.addBinding.toInstance(classOf[SpatialDistortionOp])
    ops.addBinding.toInstance(classOf[TemporalDistortionOp])
    ops.addBinding.toInstance(classOf[TemporalGapSplittingOp])
    ops.addBinding.toInstance(classOf[TransmissionDelayOp])
    ops.addBinding.toInstance(classOf[TemporalSamplingOp])
    ops.addBinding.toInstance(classOf[UniformSamplingOp])
  }
}