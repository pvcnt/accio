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

import java.nio.file.{Path, Paths}

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.core.param.{ParamGrid, ParamMap}
import fr.cnrs.liris.accio.core.pipeline.JsonHelper._
import fr.cnrs.liris.common.util.{Distance, FileUtils}

import scala.collection.JavaConverters._

trait ExperimentParser {
  def parse(path: Path): ExperimentDef
}

class JsonExperimentParser @Inject()(registry: OpRegistry, workflowParser: WorkflowParser)
    extends ExperimentParser with LazyLogging {
  override def parse(path: Path): ExperimentDef = {
    val om = new ObjectMapper
    val root = om.readTree(path.toFile)
    if (root.has("workflow")) {
      getExperiment(path, root)
    } else {
      logger.warn(s"Implicitly converting a workflow definition into an experiment definition at ${path.toAbsolutePath}")
      val workflow = workflowParser.parse(path)
      new ExperimentDef(
        name = getDefaultName(path),
        workflow = workflow,
        paramMap = None,
        exploration = None,
        optimization = None,
        notes = None,
        tags = Set.empty,
        initiator = getDefaultUser)
    }
  }

  private def getExperiment(path: Path, root: JsonNode) = {
    val workflow = getWorkflow(path, root.child("workflow"))
    val name = root.getString("meta.name").getOrElse(getDefaultName(path))
    val notes = root.getString("meta.notes")
    val tags = root.getArray("meta.tags")
        .map(_.map(_.string).toSet)
        .getOrElse(Set.empty)
    val paramDefs = workflow.graph.nodes.flatMap { nodeDef =>
      registry(nodeDef.op).defn.params.map { paramDef =>
        paramDef.copy(name = s"${nodeDef.name}/${paramDef.name}")
      }
    }
    val params = root.getChild("params").map(getParamMap(paramDefs, _))
    val optimization = root.getChild("optimization").map(getOptimization(paramDefs, _))
    val exploration = root.getChild("exploration").map(getExploration(paramDefs, _))

    new ExperimentDef(
      name = name,
      workflow = workflow,
      paramMap = params,
      exploration = exploration,
      optimization = optimization,
      notes = notes,
      tags = tags,
      initiator = getDefaultUser)
  }

  private def getDefaultName(path: Path) = path.getFileName.toString.stripSuffix(".json")

  private def getDefaultUser = User(sys.env.getOrElse("ACCIO_USERNAME", sys.props("user.name")))

  private def getExploration(paramDefs: Seq[ParamDef], node: JsonNode) = {
    val paramGrid = node.getChild("grid")
        .map(getParamGrid(paramDefs, _))
        .getOrElse(ParamGrid.empty)
    new Exploration(paramGrid)
  }

  private def getWorkflow(path: Path, node: JsonNode) = {
    val str = node.asText
    if (str.startsWith("./")) {
      workflowParser.parse(path.resolveSibling(str))
    } else if (str.startsWith("/")) {
      workflowParser.parse(Paths.get(str))
    } else if (str.startsWith("~")) {
      workflowParser.parse(Paths.get(FileUtils.replaceHome(str)))
    } else {
      throw new IllegalArgumentException(s"Invalid workflow reference: $str")
    }
  }

  private def getOptimization(paramDefs: Seq[ParamDef], node: JsonNode) = {
    val objectives = node.getChild("objectives")
        .map(_.elements.asScala.map(getObjective).toSet)
        .getOrElse(Set.empty)
    val paramGrid = node.getChild("grid")
        .map(getParamGrid(paramDefs, _))
        .getOrElse(ParamGrid.empty)
    val iters = node.getInteger("iters").getOrElse(1)
    val contraction = node.getDouble("contraction").getOrElse(.5)
    new Optimization(paramGrid, iters, contraction, objectives)
  }

  private def getObjective(node: JsonNode): Objective = {
    val metric = node.string("metric")
    val threshold = node.getDouble("threshold")
    node.string("type") match {
      case "minimize" => Objective.Minimize(metric, threshold)
      case "maximize" => Objective.Maximize(metric, threshold)
      case typ => throw new IllegalArgumentException(s"Unknown objective type: $typ")
    }
  }

  private def getParamMap(paramDefs: Seq[ParamDef], node: JsonNode) = {
    val map = node.fields.asScala.map { entry =>
      val paramDef = paramDefs.find(_.name == entry.getKey)
      require(paramDef.isDefined, s"Unknown param ${entry.getKey}")
      entry.getKey -> Params.parse(paramDef.get.typ, entry.getValue)
    }.toMap
    new ParamMap(map)
  }

  private def getParamGrid(paramDefs: Seq[ParamDef], node: JsonNode) = {
    val grid = node.fields.asScala.map { entry =>
      val paramDef = paramDefs.find(_.name == entry.getKey)
      require(paramDef.isDefined, s"Unknown param ${entry.getKey}")
      val values = if (entry.getValue.has("list")) {
        entry.getValue.get("list").elements.asScala.map(Params.parse(paramDef.get.typ, _)).toArray
      } else if (entry.getValue.has("range")) {
        getRange(paramDef.get, entry.getValue.get("range"))
      } else if (entry.getValue.has("ranges")) {
        entry.getValue.get("ranges").elements.asScala.map(getRange(paramDef.get, _)).flatten.toArray
      } else {
        throw new IllegalArgumentException("A param grid must be defined either with 'list', 'range' or 'ranges'")
      }
      entry.getKey -> values
    }.toMap
    new ParamGrid(grid)
  }

  private def getRange(paramDef: ParamDef, node: JsonNode): Array[Any] = {
    val bounds = node.elements.asScala.map(Params.parse(paramDef.typ, _)).toSeq
    val range = paramDef.typ match {
      case ParamType.Double =>
        require(bounds.size == 3, "You must specifify a double range as [from, to, step]")
        val doubles = bounds.map(_.asInstanceOf[Double])
        val from = math.min(doubles(0), doubles(1))
        val to = math.max(doubles(0), doubles(1))
        from to to by doubles(2)
      case ParamType.Integer =>
        require(bounds.size == 2 || bounds.size == 3, "You must specifify an integer range either as [from, to] or [from, to, step]")
        val ints = bounds.map(_.asInstanceOf[Int])
        val step = if (ints.size == 3) ints(2) else 1
        val from = math.min(ints(0), ints(1))
        val to = math.max(ints(0), ints(1))
        from to to by step
      case ParamType.Long =>
        require(bounds.size == 2 || bounds.size == 3, "You must specifify a long range either as [from, to] or [from, to, step]")
        val longs = bounds.map(_.asInstanceOf[Long])
        val step = if (longs.size == 3) longs(2) else 1L
        val from = math.min(longs(0), longs(1))
        val to = math.max(longs(0), longs(1))
        from to to by step
      case ParamType.Distance =>
        require(bounds.size == 3, "You must specifify a distance range as [from, to, step]")
        val distances = bounds.map(_.asInstanceOf[Distance])
        val from = math.min(distances(0).meters, distances(1).meters)
        val to = math.max(distances(0).meters, distances(1).meters)
        (from to to by distances(2).meters).map(Distance.meters)
      case typ => throw new IllegalArgumentException(s"Undefined range of param type $typ")
    }
    range.toArray
  }
}