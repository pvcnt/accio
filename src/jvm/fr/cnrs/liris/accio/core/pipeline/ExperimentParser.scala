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
import java.util.UUID

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.nscala_time.time.Imports._
import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.core.framework.{ParamGrid, ParamMap}
import fr.cnrs.liris.accio.core.pipeline.JsonHelper._
import fr.cnrs.liris.common.util.{Distance, FileUtils, HashUtils}
import org.joda.time.Instant

import scala.collection.JavaConverters._

trait ExperimentParser {
  def parse(path: Path): Experiment
}

class JsonExperimentParser @Inject()(registry: OpRegistry, workflowParser: WorkflowParser)
    extends ExperimentParser with LazyLogging {

  override def parse(path: Path): Experiment = {
    require(path.toFile.exists && path.toFile.isFile, s"${path.toAbsolutePath} does not seem to be a valid file")
    require(path.toFile.canRead, s"${path.toAbsolutePath} is not readable")

    val om = new ObjectMapper
    val root = om.readTree(path.toFile)
    if (root.has("workflow")) {
      getExperiment(path, root)
    } else {
      logger.warn(s"Implicitly converting a workflow definition into an experiment definition at ${path.toAbsolutePath}")
      val workflow = workflowParser.parse(path).setRuns(root.getInteger("runs").getOrElse(1))
      val id = HashUtils.sha1(UUID.randomUUID().toString)
      new Experiment(
        id = id,
        name = getDefaultName(path),
        workflow = workflow,
        paramMap = None,
        exploration = None,
        notes = None,
        tags = Set.empty,
        initiator = getDefaultUser)
    }
  }

  private def getExperiment(path: Path, root: JsonNode) = {
    val id = HashUtils.sha1(UUID.randomUUID().toString)
    val workflow = getWorkflow(path, root.child("workflow"))
    val name = root.getString("name").getOrElse(getDefaultName(path))
    val notes = root.getString("notes")
    val tags = root.getArray("tags")
        .map(_.map(_.string).toSet)
        .getOrElse(Set.empty)
    val paramDefs = workflow.graph.nodes.flatMap { nodeDef =>
      registry(nodeDef.op).defn.params.map { paramDef =>
        paramDef.copy(name = s"${nodeDef.name}/${paramDef.name}")
      }
    }
    val params = root.getChild("params").map(getParamMap(paramDefs, _))
    val exploration = root.getChild("exploration").map(getExploration(paramDefs, _))

    new Experiment(
      id = id,
      name = name,
      workflow = workflow,
      paramMap = params,
      exploration = exploration,
      notes = notes,
      tags = tags,
      initiator = getDefaultUser)
  }

  private def getDefaultName(path: Path) = path.getFileName.toString.stripSuffix(".json")

  private def getDefaultUser =
    sys.env.get("ACCIO_USER").map(User.parse).getOrElse(User(sys.props("user.name")))

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
    val range = paramDef.typ match {
      case ParamType.Double =>
        val bounds = node.elements.asScala.map(Params.parse(paramDef.typ, _).asInstanceOf[Double]).toSeq
        require(bounds.size == 3, "You must specify a double range as [from, to, step]")
        val from = math.min(bounds(0), bounds(1))
        val to = math.max(bounds(0), bounds(1))
        from to to by bounds(2)
      case ParamType.Integer =>
        val bounds = node.elements.asScala.map(Params.parse(paramDef.typ, _).asInstanceOf[Int]).toSeq
        require(bounds.size == 2 || bounds.size == 3, "You must specify an integer range either as [from, to] or [from, to, step]")
        val step = if (bounds.size == 3) bounds(2) else 1
        val from = math.min(bounds(0), bounds(1))
        val to = math.max(bounds(0), bounds(1))
        from to to by step
      case ParamType.Long =>
        val bounds = node.elements.asScala.map(Params.parse(paramDef.typ, _).asInstanceOf[Long]).toSeq
        require(bounds.size == 2 || bounds.size == 3, "You must specify a long range either as [from, to] or [from, to, step]")
        val from = math.min(bounds(0), bounds(1))
        val to = math.max(bounds(0), bounds(1))
        val step = if (bounds.size == 3) bounds(2) else 1L
        from to to by step
      case ParamType.Distance =>
        val bounds = node.elements.asScala.map(Params.parse(paramDef.typ, _).asInstanceOf[Distance]).toSeq
        require(bounds.size == 3, "You must specify a distance range as [from, to, step]")
        val from = math.min(bounds(0).meters, bounds(1).meters)
        val to = math.max(bounds(0).meters, bounds(1).meters)
        (from to to by bounds(2).meters).map(Distance.meters)
      case ParamType.Duration =>
        val bounds = node.elements.asScala.map(Params.parse(paramDef.typ, _).asInstanceOf[Duration]).toSeq
        require(bounds.size == 3, "You must specify a duration range as [from, to, step]")
        val from = math.min(bounds(0).getMillis, bounds(1).getMillis)
        val to = math.max(bounds(0).getMillis, bounds(1).getMillis)
        (from to to by bounds(2).getMillis).map(new Duration(_))
      case ParamType.Timestamp =>
        val bounds = node.elements.asScala.toSeq
        require(node.elements.asScala.size == 3, "You must specify a duration range as [from, to, step]")
        val timestamps = Seq(Params.parse(ParamType.Timestamp, bounds(0)), Params.parse(ParamType.Timestamp, bounds(1))).map(_.asInstanceOf[Instant])
        val from = math.min(timestamps(0).millis, timestamps(1).millis)
        val to = math.max(timestamps(0).millis, timestamps(1).millis)
        val step = Params.parse(ParamType.Duration, bounds(2)).asInstanceOf[Duration]
        (from to to by step.millis).map(new Instant(_))
      case typ => throw new IllegalArgumentException(s"Undefined range of param type $typ")
    }
    range.toArray
  }
}