package fr.cnrs.liris.accio.core.pipeline

import com.fasterxml.jackson.databind.JsonNode
import fr.cnrs.liris.accio.core.framework.ParamType
import fr.cnrs.liris.common.util.Distance
import org.joda.time.{Duration, Instant}

import scala.collection.JavaConverters._

object Params {
  def parse(typ: ParamType, node: JsonNode) = typ match {
    case ParamType.Boolean => node.asBoolean
    case ParamType.Integer =>
      require(node.isNumber, "Not a number")
      node.asInt
    case ParamType.Double =>
      require(node.isNumber, "Not a number")
      node.asDouble
    case ParamType.Long =>
      require(node.isNumber, "Not a number")
      node.asLong
    case ParamType.Distance =>
      require(node.isTextual, "Not a distance string")
      Distance.parse(node.asText)
    case ParamType.Duration =>
      require(node.isTextual, "Not a duration string")
      new Duration(com.twitter.util.Duration.parse(node.asText).inMillis)
    case ParamType.Timestamp =>
      require(node.isTextual, "Not a datetime string")
      Instant.parse(node.asText)
    case ParamType.String => node.asText
    case ParamType.StringList => node.elements.asScala.map(_.asText).toSeq
  }

  def parse(typ: ParamType, str: String) = typ match {
    case ParamType.Boolean => str match {
      case "t" | "true" | "y" | "yes" => true
      case "f" | "false" | "n" | "no" => false
      case _ => throw new IllegalArgumentException(s"Invalid boolean: $str")
    }
    case ParamType.Integer => str.toInt
    case ParamType.Double => str.toDouble
    case ParamType.Long => str.toLong
    case ParamType.Distance => Distance.parse(str)
    case ParamType.Duration => new Duration(com.twitter.util.Duration.parse(str).inMillis)
    case ParamType.Timestamp => Instant.parse(str)
    case ParamType.String => str
    case ParamType.StringList => str.split(",")
  }
}