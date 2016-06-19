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
import fr.cnrs.liris.accio.core.param._
import fr.cnrs.liris.common.util.FileUtils

import scala.collection.JavaConverters._

trait WorkflowParser {
  def parse(path: Path): WorkflowDef
}

class JsonWorkflowParser @Inject()(registry: OpRegistry) extends WorkflowParser {
  override def parse(path: Path): WorkflowDef = {
    val om = new ObjectMapper
    val root = om.readTree(path.toFile)

    val id = if (root.hasNonNull("id")) {
      root.get("id").asText
    } else {
      FileUtils.removeExtension(path.getFileName.toString)
    }
    val version = if (root.hasNonNull("version")) getVersion(root.get("version")) else "1"
    val (name, owner) = if (root.has("meta")) {
      val meta = root.get("meta")
      val name = if (meta.hasNonNull("name")) Some(meta.get("name").asText) else None
      val owner = if (meta.hasNonNull("owner")) Some(User.parse(meta.get("owner").asText)) else None
      (name, owner)
    } else {
      (None, None)
    }
    val nodes = root.get("graph").elements.asScala.map(getNode).toSeq

    new WorkflowDef(id, version, new GraphDef(nodes), name, owner)
  }

  private def getVersion(node: JsonNode) = if (node.isInt) node.asInt.toString else node.asText

  private def getNode(node: JsonNode) = {
    val opName = node.get("op").asText
    require(registry.contains(opName), s"Unknown operator $opName")
    val opMeta = registry(opName)
    val name = if (node.hasNonNull("name")) node.get("name").asText else opName
    val rawParams = if (node.hasNonNull("params")) {
      node.get("params").fields.asScala.map(entry => entry.getKey -> entry.getValue).toMap
    } else {
      Map.empty[String, JsonNode]
    }
    val params = opMeta.defn.params.map { paramDef =>
      val maybeValue = rawParams.get(paramDef.name).map(Params.parse(paramDef.typ, _)).orElse(paramDef.defaultValue)
      require(maybeValue.isDefined, s"Param $name/${paramDef.name} is not defined")
      paramDef.name -> maybeValue.get
    }
    val inputs = if (node.hasNonNull("inputs")) {
      node.get("inputs").elements.asScala.map(_.asText).toSeq
    } else {
      Seq.empty[String]
    }
    new NodeDef(opName, name, new ParamMap(params.toMap), inputs, 1)
  }
}