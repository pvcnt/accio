package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.inject.Singleton

@Singleton
class JsonReportCodec extends ReportWriter with ReportReader {
  private[this] val om = {
    val om = new ObjectMapper
    om.registerModules(new DefaultScalaModule, new JodaModule, new JacksonAccioModule)
    om.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    om.setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
    om.enable(SerializationFeature.INDENT_OUTPUT)
    om
  }

  override def readExperiment(workDir: Path, id: String): ExperimentRun =
    om.reader.withType(classOf[ExperimentRun]).readValue[ExperimentRun](workDir.resolve(s"experiment-$id.json").toFile)

  override def readRun(workDir: Path, id: String): Run =
    om.reader.withType(classOf[Run]).readValue[Run](workDir.resolve(s"run-$id.json").toFile)

  override def write(workDir: Path, experiment: ExperimentRun): Unit =
    om.writer.writeValue(workDir.resolve(s"experiment-${experiment.id}.json").toFile, experiment)

  override def write(workDir: Path, run: Run): Unit =
    om.writer.writeValue(workDir.resolve(s"run-${run.id}.json").toFile, run)
}