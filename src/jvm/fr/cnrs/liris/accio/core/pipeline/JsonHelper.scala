package fr.cnrs.liris.accio.core.pipeline

import com.fasterxml.jackson.databind.JsonNode
import scala.collection.JavaConverters._

class RichJsonNode(node: JsonNode) {
  def integer: Int = integer("")

  def integer(path: String): Int =
    getInteger(path) match {
      case Some(v) => v
      case None => throw new IllegalArgumentException(s"$path is not an integer")
    }

  def long: Long = long("")

  def long(path: String): Long =
    getLong(path) match {
      case Some(v) => v
      case None => throw new IllegalArgumentException(s"$path is not a long")
    }

  def double: Double = double("")

  def double(path: String): Double =
    getDouble(path) match {
      case Some(v) => v
      case None => throw new IllegalArgumentException(s"$path is not a double")
    }

  def string: String = string("")

  def string(path: String): String =
    getString(path) match {
      case Some(v) => v
      case None => throw new IllegalArgumentException(s"$path is not a string")
    }

  def boolean: Boolean = boolean("")

  def boolean(path: String): Boolean =
    getBoolean(path) match {
      case Some(v) => v
      case None => throw new IllegalArgumentException(s"$path is not a boolean")
    }

  def array: Seq[JsonNode] = array("")

  def array(path: String): Seq[JsonNode] =
    getArray(path) match {
      case Some(v) => v
      case None => throw new IllegalArgumentException(s"$path is not an array")
    }

  def getInteger(path: String): Option[Int] = getChild(path).filter(_.isInt).map(_.asInt)

  def getLong(path: String): Option[Long] = getChild(path).filter(_.isLong).map(_.asLong)

  def getDouble(path: String): Option[Double] = getChild(path).filter(_.isDouble).map(_.asDouble)

  def getString(path: String): Option[String] = getChild(path).filter(_.isTextual).map(_.asText)

  def getBoolean(path: String): Option[Boolean] =
    getChild(path).filter(_.isBoolean).map(_.asBoolean)

  def getArray(path: String): Option[Seq[JsonNode]] =
    getChild(path).filter(_.isArray).map(_.elements.asScala.toSeq)

  def child(path: String): JsonNode =
    getChild(path) match {
      case Some(n) => n
      case None => throw new IllegalArgumentException(s"$path is not a node")
    }

  def getChild(path: String): Option[JsonNode] = {
    if (path.isEmpty) {
      Some(node)
    } else {
      var res: Option[JsonNode] = Some(node)
      for (part <- path.split("\\.")) {
        if (res.isDefined && res.get.hasNonNull(part)) {
          res = Some(res.get.get(part))
        } else {
          res = None
        }
      }
      res
    }
  }
}

object JsonHelper {
  implicit def toRichJsonNode(node: JsonNode): RichJsonNode = new RichJsonNode(node)
}
