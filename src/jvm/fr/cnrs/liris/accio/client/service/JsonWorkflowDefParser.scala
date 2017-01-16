package fr.cnrs.liris.accio.client.service

import java.io.FileInputStream
import java.nio.file.Path

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonSubTypes}
import com.google.inject.Inject
import com.twitter.finatra.json.FinatraObjectMapper
import com.typesafe.scalalogging.LazyLogging

import scala.util.control.NonFatal

private[service] class JsonWorkflowDefParser @Inject()(mapper: FinatraObjectMapper) extends LazyLogging {
  @throws[ParsingException]
  def parse(path: Path): JsonWorkflowDef = {
    val file = path.toFile
    if (!file.exists || !file.canRead) {
      throw new ParsingException(s"Cannot read workflow definition file ${path.toAbsolutePath}")
    }
    val fis = new FileInputStream(file)
    try {
      mapper.parse[JsonWorkflowDef](fis)
    } catch {
      case NonFatal(e) => throw new ParsingException("Error while parsing workflow definition", e)
    } finally {
      fis.close()
    }
  }
}

private[service] case class JsonWorkflowDef(
  graph: Seq[JsonNodeDef],
  id: Option[String],
  owner: Option[String],
  name: Option[String],
  params: Seq[JsonArgDef] = Seq.empty)

private[service] case class JsonArgDef(name: String, kind: String, defaultValue: Option[Any])

private[service] case class JsonNodeDef(op: String, name: Option[String], inputs: Map[String, JsonInputDef] = Map.empty)

@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[JsonValueInputDef], name = "value"),
  new JsonSubTypes.Type(value = classOf[JsonReferenceInputDef], name = "reference"),
  new JsonSubTypes.Type(value = classOf[JsonParamInputDef], name = "param")))
@JsonIgnoreProperties(ignoreUnknown = true)
private[service] sealed trait JsonInputDef

private[service] case class JsonValueInputDef(value: Any) extends JsonInputDef

private[service] case class JsonReferenceInputDef(reference: String) extends JsonInputDef

private[service] case class JsonParamInputDef(param: String) extends JsonInputDef