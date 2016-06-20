package fr.cnrs.liris.accio.core.pipeline

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.framework.{OpRegistry, Operator}
import fr.cnrs.liris.accio.core.param.ParamMap
import fr.cnrs.liris.common.util.Named

case class GraphDef(nodes: Seq[NodeDef]) {
  def apply(name: String): NodeDef = nodes.find(_.name == name).get

  def params: ParamMap = {
    val map = nodes.flatMap { node =>
      node.paramMap.toSeq.map { case (name, value) => s"${node.name}/$name" -> value }
    }.toMap
    new ParamMap(map)
  }

  /**
   * Return a copy of this graph with new parameters propagated into the nodes.
   *
   * @param paramMap Override parameters map
   * @return A new graph
   */
  def setParams(paramMap: ParamMap): GraphDef = {
    val newNodes = nodes.map { nodeDef =>
      nodeDef.copy(paramMap = nodeDef.paramMap ++ paramMap.filter(nodeDef.name))
    }
    copy(nodes = newNodes)
  }

  /**
   * Return a copy of this graph with a minimum number of runs propagated into the nodes.
   *
   * @param runs Minimum number of runs
   * @return A new graph
   */
  def requireRuns(runs: Int): GraphDef = {
    if (runs > 1) {
      val newNodes = nodes.map(node => node.copy(runs = math.max(node.runs, runs)))
      copy(nodes = newNodes)
    } else this
  }
}

case class NodeDef(op: String, name: String, paramMap: ParamMap, inputs: Seq[String], runs: Int) extends Named

class Graph(_nodes: Map[String, Node]) {
  def apply(name: String): Node = _nodes(name)

  def nodes: Set[Node] = _nodes.values.toSet

  def roots: Set[Node] = nodes.filter(_.dependencies.isEmpty)
}

case class Node(name: String, operator: Operator, dependencies: Seq[String], successors: Set[String], runs: Int) extends Named

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
    require(registry.contains(nodeDef.op), s"Unknown operator '${nodeDef.op}'")
    val opMeta = registry(nodeDef.op)
    val args = opMeta.defn.params.map { paramDef =>
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
    val operator = opMeta.clazz.getConstructors.head.newInstance(args: _*).asInstanceOf[Operator]
    new Node(nodeDef.name, operator, nodeDef.inputs, Set.empty, 1)
  }
}