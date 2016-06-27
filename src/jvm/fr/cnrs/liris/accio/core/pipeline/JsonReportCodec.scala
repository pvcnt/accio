package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, ObjectMapper, SerializerProvider}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.inject.Singleton
import fr.cnrs.liris.accio.core.param.ParamMap

@Singleton
class JsonReportCodec extends ReportWriter with ReportReader {
  private[this] val om = {
    val om = new ObjectMapper
    om.registerModules(new DefaultScalaModule, new JodaModule, new JacksonAccioModule)
    om
  }

  override def readExperiment(workDir: Path, id: String): ExperimentRun =
    om.reader.withType(classOf[ExperimentRun]).readValue[ExperimentRun](workDir.resolve(s"experiment-$id.json").toFile)

  override def readRun(workDir: Path, id: String): WorkflowRun =
    om.reader.withType(classOf[WorkflowRun]).readValue[WorkflowRun](workDir.resolve(s"run-$id.json").toFile)

  override def write(workDir: Path, experiment: ExperimentRun): Unit =
    om.writer.writeValue(workDir.resolve(s"experiment-${experiment.id}.json").toFile, experiment)

  override def write(workDir: Path, run: WorkflowRun): Unit =
    om.writer.writeValue(workDir.resolve(s"run-${run.id}.json").toFile, run)
}

private class JacksonAccioModule extends SimpleModule {
  addSerializer(new ParamMapSerializer)
  addDeserializer(classOf[ParamMap], new ParamMapDeserializer)
}

private class ParamMapSerializer extends StdSerializer[ParamMap](classOf[ParamMap]) {
  override def serialize(paramMap: ParamMap, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeObject(paramMap.toMap)
  }
}

private class ParamMapDeserializer extends StdDeserializer[ParamMap](classOf[ParamMap]) {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): ParamMap = {
    val map = jsonParser.readValueAs(classOf[Map[String, Any]])
    new ParamMap(map)
  }
}