package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.inject.Singleton
import fr.cnrs.liris.accio.core.framework._

@Singleton
class JsonExperimentCodec extends ExperimentWriter with ExperimentReader {
  private[this] val om = {
    val om = new ObjectMapper
    om.registerModules(new DefaultScalaModule, new JodaModule)
    om
  }

  override def readExperiment(workDir: Path, id: String): Experiment =
    om.reader.readValue[Experiment](workDir.resolve(s"experiment-$id.json").toFile)

  override def readWorkflowRun(workDir: Path, id: String): WorkflowRun =
    om.reader.readValue[WorkflowRun](workDir.resolve(s"run-$id.json").toFile)

  override def write(workDir: Path, experiment: Experiment): Unit =
    om.writer.writeValue(workDir.resolve(s"experiment-${experiment.id}.json").toFile, experiment)

  override def write(workDir: Path, run: WorkflowRun): Unit =
    om.writer.writeValue(workDir.resolve(s"run-${run.id}.json").toFile, run)
}