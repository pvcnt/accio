package fr.cnrs.liris.accio.core.framework

import com.fasterxml.jackson.annotation.{JsonProperty, JsonValue}
import fr.cnrs.liris.common.util.Named

import scala.annotation.meta.getter

/**
 * Definition of a graph.
 *
 * You should use a [[GraphFactory]] to convert a [[GraphDef]] into a [[Graph]].
 *
 * @param nodes Definition of nodes forming this graph.
 */
case class GraphDef(@(JsonValue@getter) nodes: Seq[NodeDef])

/**
 * Definition of a node inside a graph.
 *
 * @param op         Operator name.
 * @param customName Node name (by default it will be the operator name).
 * @param inputs     Inputs of the operator.
 */
case class NodeDef(
  op: String,
  @JsonProperty("name") customName: Option[String] = None,
  inputs: Map[String, Input] = Map.empty) extends Named {

  /**
   * Return the actual name of the node.
   */
  override def name: String = customName.getOrElse(op)
}
