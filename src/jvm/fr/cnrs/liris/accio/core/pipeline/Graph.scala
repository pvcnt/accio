package fr.cnrs.liris.accio.core.pipeline

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.framework.{OpMeta, OpRegistry, Operator}
import fr.cnrs.liris.accio.core.param.ParamMap
import fr.cnrs.liris.common.util.Named

/**
 * Definition of a graph. Definitions must be converted into [[Graph]]'s in order to be executed. No validation is done
 * inside graph definitions.
 *
 * @param nodes Definitions of nodes composing this graph
 */
case class GraphDef(nodes: Seq[NodeDef]) {
  /**
   * Return the definition of the node with a given name.
   *
   * @param name Node name
   * @throws NoSuchElementException If there is no node with this name
   */
  @throws[NoSuchElementException]
  def apply(name: String): NodeDef = nodes.find(_.name == name).get

  /**
   * Return the parameters map for all nodes composing this graph.
   */
  def params: ParamMap = {
    val map = nodes.flatMap { node =>
      node.paramMap.toSeq.map { case (name, value) => s"${node.name}/$name" -> value }
    }.toMap
    new ParamMap(map)
  }

  /**
   * Check whether another graph definition has the same structure of this one, i.e., all nodes are connected in a
   * similar manner.
   *
   * @param other Another graph definition
   * @return True if they have the same structure, false otherwise
   */
  def hasSameStructure(other: GraphDef): Boolean =
    other.nodes.size == nodes.size && nodes.forall(n => other.nodes.exists(_.hasSameStructure(n)))

  /**
   * Return the size of this graph, i.e., the number of nodes composing it.
   */
  def size: Int = nodes.size

  /**
   * Return a copy of this graph with new parameters propagated into the nodes.
   *
   * @param paramMap Override parameters map
   */
  def setParams(paramMap: ParamMap): GraphDef = {
    val newNodes = nodes.map { nodeDef =>
      nodeDef.copy(paramMap = nodeDef.paramMap ++ paramMap.filter(nodeDef.name))
    }
    copy(nodes = newNodes)
  }

  /**
   * Return a copy of this graph with a minimum number of runs propagated into the nodes that would otherwise run only
   * one time. For nodes for which an explicit number of runs is already set, this value will not be overriden.
   *
   * @param runs Minimum number of runs
   */
  def setRuns(runs: Int): GraphDef = {
    if (runs > 1) {
      val newNodes = nodes.map { node =>
        if (node.runs <= 1) {
          node.copy(runs = math.max(node.runs, runs))
        } else {
          node
        }
      }
      copy(nodes = newNodes)
    } else this
  }
}

case class NodeDef(op: String, name: String, paramMap: ParamMap, inputs: Seq[String] = Seq.empty, runs: Int = 1) extends Named {
  def hasSameStructure(other: NodeDef): Boolean =
    other.op == op && other.name == name && other.inputs == inputs
}

class Graph(_nodes: Map[String, Node]) {
  def apply(name: String): Node = _nodes(name)

  def nodes: Set[Node] = _nodes.values.toSet

  def roots: Set[Node] = nodes.filter(_.dependencies.isEmpty)

  def size: Int = _nodes.size
}

case class Node(defn: NodeDef, operator: Operator[_, _], dependencies: Seq[String], successors: Set[String]) extends Named {
  override def name: String = defn.name

  def runs: Int = defn.runs
}

/**
 * A graph builder builds [[Graph]]'s from [[GraphDef]]'s.
 *
 * @param registry Operator registry
 */
class GraphBuilder @Inject()(registry: OpRegistry) {
  def build(graphDef: GraphDef): Graph = {
    var nodes = graphDef.nodes.map(nodeDef => nodeDef.name -> getNode(nodeDef)).toMap
    nodes = nodes.map { case (name, node) =>
      val successors = nodes.values.filter(_.dependencies.contains(node.name)).map(_.name).toSet
      name -> node.copy(successors = successors)
    }
    new Graph(nodes)
  }

  private def getNode(nodeDef: NodeDef) = {
    require(registry.contains(nodeDef.op), s"Unknown operator: ${nodeDef.op}")
    val opMeta = registry(nodeDef.op)
    val args = getConstructorArgs(opMeta, nodeDef)
    val operator = opMeta.clazz.getConstructors.head.newInstance(args: _*).asInstanceOf[Operator[_, _]]
    new Node(nodeDef, operator, nodeDef.inputs, Set.empty)
  }

  private def getConstructorArgs(opMeta: OpMeta, nodeDef: NodeDef) = {
    val invalidParams = nodeDef.paramMap.keys.diff(opMeta.defn.params.map(_.name).toSet)
    require(invalidParams.isEmpty, s"Unknown params: ${invalidParams.map(n => s"${nodeDef.name}/$n").mkString(", ")}")
    opMeta.defn.params.map { paramDef =>
      val maybeValue = nodeDef.paramMap.get(paramDef.name).orElse(paramDef.defaultValue)
      require(maybeValue.isDefined, s"Param ${nodeDef.name}/${paramDef.name} is not defined")
      if (paramDef.optional) {
        maybeValue match {
          case Some(opt: Option[_]) => opt
          case Some(s) => Some(s)
          case a => Some(a)
        }
      } else maybeValue.get
    }.map(_.asInstanceOf[AnyRef])
  }
}