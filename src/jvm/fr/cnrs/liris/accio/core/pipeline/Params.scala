package fr.cnrs.liris.accio.core.pipeline

import com.fasterxml.jackson.databind.JsonNode
import fr.cnrs.liris.accio.core.framework.ParamType
import fr.cnrs.liris.common.util.Distance
import org.joda.time.{Duration, Instant}

import scala.collection.JavaConverters._

object Params {
  def parse(typ: ParamType, node: JsonNode) = typ match {
    case ParamType.Boolean => node.asBoolean
    case ParamType.Integer => node.asInt
    case ParamType.Double => node.asDouble
    case ParamType.Distance => Distance.parse(node.asText)
    case ParamType.Duration => new Duration(com.twitter.util.Duration.parse(node.asText).inMillis)
    case ParamType.Timestamp => Instant.parse(node.asText)
    case ParamType.Long => node.asLong
    case ParamType.String => node.asText
    case ParamType.StringList => node.elements.asScala.map(_.asText).toSeq
  }
}