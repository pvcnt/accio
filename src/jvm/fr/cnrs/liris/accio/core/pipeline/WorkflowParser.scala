/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.google.inject.Inject
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.core.pipeline.JsonHelper._

import scala.collection.JavaConverters._

/**
 * Parser for workflow definitions.
 */
trait WorkflowParser {
  /**
   * Parse a file into a workflow definition.
   *
   * @param path Path to a workflow definition
   * @return A workflow definition
   */
  def parse(path: Path): Workflow
}

/**
 * Parser for workflow definitions stored into JSON files.
 *
 * @param registry Operator registry
 */
class JsonWorkflowParser @Inject()(registry: OpRegistry) extends WorkflowParser {
  override def parse(path: Path): Workflow = {
    val om = new ObjectMapper
    val root = om.readTree(path.toFile)

    val name = root.getString("name")
    val owner = root.getString("owner").map(User.parse)
    val nodes = root.child("graph").elements.asScala.map(getNode).toSeq

    var defn = new Workflow(new GraphDef(nodes), name, owner)
    root.getInteger("runs").foreach { runs =>
      defn = defn.setRuns(runs)
    }
    defn
  }

  private def getNode(node: JsonNode) = {
    val opName = node.string("op")
    require(registry.contains(opName), s"Unknown operator: $opName")
    val opMeta = registry(opName)
    val name = node.getString("name").getOrElse(opName)
    val params = getParams(opMeta, name, node.getChild("params"))
    val inputs = getInputs(node.getChild("inputs"))
    val runs = node.getInteger("runs").getOrElse(1)
    new NodeDef(opName, name, new ParamMap(params.toMap), inputs, runs)
  }

  private def getParams(opMeta: OpMeta, name: String, node: Option[JsonNode]) = {
    val invalidParams = node.map(_.fields.asScala.map(_.getKey).toSet).getOrElse(Set.empty).diff(opMeta.defn.params.map(_.name).toSet)
    require(invalidParams.isEmpty, s"Unknown param(s): ${invalidParams.map(n => s"$name/$n").mkString(", ")}")
    val rawParams = node.map(_.fields.asScala.map(entry => entry.getKey -> entry.getValue).toMap).getOrElse(Map.empty)
    opMeta.defn.params.map { paramDef =>
      val maybeValue = rawParams.get(paramDef.name).map(Params.parse(paramDef.typ, _)).orElse(paramDef.defaultValue)
      require(maybeValue.isDefined, s"Param $name/${paramDef.name} is not defined")
      paramDef.name -> maybeValue.get
    }
  }

  private def getInputs(node: Option[JsonNode]) = node.map(_.elements.asScala.map(_.asText).toSeq).getOrElse(Seq.empty)
}