package fr.cnrs.liris.accio.client.parser

import java.io.FileInputStream
import java.nio.file.Path

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonSubTypes}
import com.google.inject.Inject
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.util.Await
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.service.handler.ListOperatorsRequest

class WorkflowTemplateParser @Inject()(mapper: FinatraObjectMapper, agentClient: AgentService.FinagledClient) extends LazyLogging {
  def parse(path: Path): WorkflowTemplate = {
    val ops = Await.result(agentClient.listOperators(ListOperatorsRequest(true))).results

    val fis = new FileInputStream(path.toFile)
    val json = try {
      mapper.parse[ParsedWorkflowTemplate](fis)
    } finally {
      fis.close()
    }
    val id = WorkflowId(json.id.getOrElse {
      val filename = path.getFileName.toString
      filename.substring(0, filename.indexOf("."))
    })
    val owner = json.owner.map(Utils.parseUser).getOrElse(Utils.DefaultUser)
    val nodes = json.graph.map { node =>
      val inputs = node.inputs.map {
        case (argName, ParsedValueInput(value)) =>
          val op = ops.find(_.name == node.op).get
          argName -> InputDef.Value(Values.encode(value, op.inputs.find(_.name == argName).get.kind))
        case (argName, ParsedReferenceInput(ref)) => argName -> InputDef.Reference(Utils.parseReference(ref))
        case (argName, ParsedParamInput(paramName)) => argName -> InputDef.Param(paramName)
      }
      NodeDef(node.op, node.name.getOrElse(node.op), inputs)
    }.toSet

    val params = json.params.map { paramDef =>
      val kind = Utils.parseDataType(paramDef.kind)
      val defaultValue = paramDef.defaultValue.map(Values.encode(_, kind))
      ArgDef(name = paramDef.name, kind = kind, defaultValue = defaultValue, isOptional = false)
    }.toSet

    WorkflowTemplate(
      id = id,
      params = params,
      name = json.name,
      owner = Some(owner),
      graph = GraphDef(nodes))
  }
}

private case class ParsedWorkflowTemplate(
  graph: Seq[ParsedNodeDef],
  id: Option[String],
  owner: Option[String],
  name: Option[String],
  params: Seq[ParsedParamDef] = Seq.empty)

private case class ParsedParamDef(name: String, kind: String, defaultValue: Option[Any])

private case class ParsedNodeDef(op: String, name: Option[String], inputs: Map[String, ParsedInput] = Map.empty)

@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[ParsedValueInput], name = "value"),
  new JsonSubTypes.Type(value = classOf[ParsedReferenceInput], name = "reference"),
  new JsonSubTypes.Type(value = classOf[ParsedParamInput], name = "param")))
@JsonIgnoreProperties(ignoreUnknown = true)
sealed trait ParsedInput

private case class ParsedValueInput(value: Any) extends ParsedInput

private case class ParsedReferenceInput(reference: String) extends ParsedInput

private case class ParsedParamInput(param: String) extends ParsedInput