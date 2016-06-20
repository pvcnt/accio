package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path

import com.fasterxml.jackson.databind.{ObjectMapper, PropertyNamingStrategy}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.inject.Singleton
import fr.cnrs.liris.accio.core.framework._

@Singleton
class JsonExperimentCodec extends ExperimentWriter with ExperimentReader {
  private[this] val om = {
    val om = new ObjectMapper
    om.registerModules(new DefaultScalaModule, new JodaModule)
    om.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
    om
  }

  override def readExperiment(workDir: Path, id: String): Experiment =
    om.reader.readValue[Experiment](workDir.resolve(s"experiment-$id.json").toFile)

  override def readRun(workDir: Path, id: String): Run =
    om.reader.readValue[Run](workDir.resolve(s"run-$id.json").toFile)

  override def write(workDir: Path, experiment: Experiment): Unit =
    om.writer.writeValue(workDir.resolve(s"experiment-${experiment.id}.json").toFile, experiment)

  override def write(workDir: Path, run: Run): Unit =
    om.writer.writeValue(workDir.resolve(s"run-${run.id}.json").toFile, run)
}