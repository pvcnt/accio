package fr.cnrs.liris.accio.cli

import com.google.inject.{AbstractModule, Provides, Singleton}
import fr.cnrs.liris.accio.cli.commands._
import fr.cnrs.liris.accio.core.dataset.DatasetEnv
import fr.cnrs.liris.accio.core.framework.OpRegistry
import fr.cnrs.liris.accio.core.ops.eval.{TransmissionDelay, _}
import fr.cnrs.liris.accio.core.ops.source.EventSource
import fr.cnrs.liris.accio.core.ops.transform.{SplitSequentially, TemporalSampling, UniformSampling, _}
import fr.cnrs.liris.accio.core.pipeline._
import net.codingwell.scalaguice.ScalaModule

class AccioModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[ExperimentParser].to[JsonExperimentParser]
    bind[WorkflowParser].to[JsonWorkflowParser]
    bind[ExperimentExecutor].to[LocalExperimentExecutor]
    bind[ExperimentWriter].to[JsonExperimentCodec]
    bind[ExperimentReader].to[JsonExperimentCodec]
  }

  @Provides
  def providesDatasetEnv: DatasetEnv = {
    new DatasetEnv(math.max(1, sys.runtime.availableProcessors() - 1))
  }

  @Provides
  @Singleton
  def providesOpRegistry: OpRegistry = {
    val registry = new OpRegistry
    // Sources
    registry.register[EventSource]

    // Transformers
    registry.register[CollapseTemporalGaps]
    registry.register[GaussianKernelSmoothing]
    registry.register[MaxDuration]
    registry.register[MinDuration]
    registry.register[MinSize]
    registry.register[SpatialSampling]
    registry.register[SplitByDuration]
    registry.register[SplitBySize]
    registry.register[SplitBySpatialGap]
    registry.register[SplitByTemporalGap]
    registry.register[SplitSequentially]
    registry.register[TemporalSampling]
    registry.register[UniformSampling]
    registry.register[GeoIndistinguishability]
    registry.register[PromesseOp]

    // Evaluators
    registry.register[PoisRetrieval]
    registry.register[SpatialDistortion]
    registry.register[TemporalDistortion]
    registry.register[AreaCoverage]
    registry.register[DataCompleteness]
    registry.register[TransmissionDelay]
    registry.register[BasicAnalyzer]
    registry.register[PoisAnalyzer]

    registry
  }

  @Provides
  def providesCommandRegistry: CommandRegistry = {
    val registry = new CommandRegistry
    registry.register[AnalyzeCommand]
    registry.register[RunCommand]
    registry.register[SummarizeCommand]
    registry.register[HelpCommand]
    registry.register[OpsCommand]
    registry
  }
}