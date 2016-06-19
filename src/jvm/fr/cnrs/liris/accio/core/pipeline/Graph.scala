package fr.cnrs.liris.accio.core.pipeline

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.framework.{GraphDef, NodeDef, OpRegistry, Operator}
import fr.cnrs.liris.common.util.Named

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