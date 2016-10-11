package fr.cnrs.liris.accio.cli

import com.google.inject.{AbstractModule, Provides, TypeLiteral}
import fr.cnrs.liris.accio.core.dataset.DatasetEnv
import fr.cnrs.liris.accio.core.framework.{AnnotationOpMetaReader, OpMetaReader, Operator}
import fr.cnrs.liris.accio.core.pipeline._
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}

object AccioModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Class[_ <: Operator[_, _]]] {})

    bind[OpMetaReader].to[AnnotationOpMetaReader]
    bind[ExperimentParser].to[JsonExperimentParser]
    bind[WorkflowParser].to[JsonWorkflowParser]
    bind[ExperimentExecutor].to[LocalExperimentExecutor]
    bind[ReportWriter].to[JsonReportCodec]
    bind[ReportReader].to[JsonReportCodec]
  }

  @Provides
  def providesDatasetEnv: DatasetEnv = {
    new DatasetEnv(math.max(1, sys.runtime.availableProcessors() - 1))
  }

  @Provides
  def providesCommandRegistry: CommandRegistry = {
    val registry = new CommandRegistry
    registry.register[RunCommand]
    registry.register[VisualizeCommand]
    registry.register[HelpCommand]
    registry.register[OpsCommand]
    registry
  }
}