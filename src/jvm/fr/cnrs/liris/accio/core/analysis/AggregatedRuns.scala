/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnrs.liris.accio.core.analysis

import fr.cnrs.liris.accio.core.api.thrift._
import fr.cnrs.liris.common.geo.Distance
import fr.cnrs.liris.common.util.Seqs
import fr.cnrs.liris.common.util.MathUtils.mean
import fr.cnrs.liris.dal.core.api._
import org.joda.time.Duration

/**
 * Aggregate data about several runs.
 *
 * @param runs Non-empty list of runs to aggregate.
 */
class AggregatedRuns(val runs: Seq[Run]) {
  require(runs.nonEmpty, "You must provide some runs to aggregate")

  def size: Int = runs.size

  /**
   * Return an aggregated description of the artifacts contained inside those runs.
   */
  def artifacts: ArtifactList = {
    // Group all artifacts by run id.
    val artifactsByRun = runs.flatMap { run =>
      run.state.nodes.flatMap { node =>
        node.result.toSeq.flatMap(_.artifacts.map(art => run.id -> art.copy(name = s"${node.name}/${art.name}")))
      }
    }

    // Group artifacts by their name, and then by run id..
    val groups = artifactsByRun
      .groupBy(_._2.name)
      .map { case (artifactName, list) =>
        val values = list.map { case (runId, art) => runId -> art.value }
        ArtifactGroup(artifactName, list.head._2.value.kind, values.toMap)
      }.toSeq

    // Only keep runs for which we have at least one artifact.
    val runIds = artifactsByRun.map(_._1).toSet
    val effectiveRuns = runs.filter(run => runIds.contains(run.id))

    ArtifactList(effectiveRuns, Map.empty, groups)
  }

  /**
   * Return an aggregated description of the metrics contained inside those runs.
   */
  def metrics: MetricList = {
    // Group all metrics by run id.
    val metricsByRun = runs.flatMap { run =>
      run.state.nodes.flatMap { node =>
        node.result.toSeq.flatMap(_.metrics.map(metric => run.id -> metric.copy(name = s"${node.name}/${metric.name}")))
      }
    }

    // Group metrics by their name, and then by run id..
    val groups = metricsByRun
      .groupBy(_._2.name)
      .map { case (metricName, list) =>
        val values = list.map { case (runId, art) => runId -> art.value }
        MetricGroup(metricName, values.toMap)
      }.toSeq

    // Only keep runs for which we have at least one artifact.
    val runIds = metricsByRun.map(_._1).toSet
    val effectiveRuns = runs.filter(run => runIds.contains(run.id))

    MetricList(effectiveRuns, Map.empty, groups)
  }
}

/**
 * A list of artifacts.
 *
 * @param runs
 * @param params
 * @param groups
 */
case class ArtifactList(runs: Seq[Run], params: Map[String, Value], groups: Seq[ArtifactGroup]) {
  /**
   * Filter the artifacts being aggregated to only include those specified. The list of names specifies names of
   * artifacts to keep, plus special names: ALL (include everything), NUMERIC (include numeric types). If the list
   * is empty, no filtering will actually be applied.
   *
   * @param names List of artifact names.
   * @return A copy including only specified artifact.
   */
  def filter(names: Set[String]): ArtifactList = {
    if (names.isEmpty) {
      this
    } else if (names.contains("ALL")) {
      this
    } else {
      val includeNumeric = names.contains("NUMERIC")
      val newGroups = groups.filter { group =>
        names.contains(group.name) || (includeNumeric && DataTypes.isNumeric(group.kind))
      }
      copy(groups = newGroups)
    }
  }

  /**
   * Split this list of artifacts into several lists, one per unique workflow parametrization. If this instance has
   * already been split, it will result in a singleton composed of itself.
   */
  def split: Seq[ArtifactList] = {
    val commonParams = Seqs.index(runs.flatMap(run => run.params.toSeq))
      .filter { case (_, values) => values.toSet.size == 1 }
      .keySet
    val runsByParams = runs.groupBy(_.params)
    runsByParams.map { case (someParams, someRuns) =>
      val runIds = someRuns.map(_.id)
      val newGroups = groups.map { group =>
        val newValues = group.values.filter { case (runId, _) => runIds.contains(runId) }
        group.copy(values = newValues)
      }
      ArtifactList(someRuns, someParams.filterKeys(key => !commonParams.contains(key)).toMap, newGroups)
    }.toSeq
  }
}

/**
 * A group of artifacts.
 *
 * @param name   Artifact name.
 * @param kind   Artifact data type.
 * @param values Artifact values, keyed by run id.
 */
case class ArtifactGroup(name: String, kind: DataType, values: Map[RunId, Value]) {
  def toSeq: Seq[Artifact] = values.values.map(v => Artifact(name, v)).toSeq

  def aggregated: Artifact = Artifact(name, aggregate(kind, values.values.toSeq))

  private def aggregate(kind: DataType, values: Seq[Value]): Value = kind.base match {
    case AtomicType.Byte => Values.encodeDouble(mean(values.map(Values.decodeByte(_).toDouble)))
    case AtomicType.Integer => Values.encodeDouble(mean(values.map(Values.decodeInteger(_).toDouble)))
    case AtomicType.Double => Values.encodeDouble(mean(values.map(Values.decodeDouble)))
    case AtomicType.Long => Values.encodeDouble(mean(values.map(Values.decodeLong(_).toDouble)))
    case AtomicType.Distance =>
      Values.encodeDistance(Distance.meters(mean(values.map(Values.decodeDistance(_).meters))))
    case AtomicType.Duration =>
      Values.encodeDuration(Duration.millis(mean(values.map(Values.decodeDuration(_).getMillis.toDouble)).round))
    case AtomicType.List =>
      val of = DataType(kind.args.head)
      aggregate(of, values.flatMap(Values.decodeList).map(Values.encode(_, of)))
    case AtomicType.Set =>
      val of = DataType(kind.args.head)
      aggregate(of, values.flatMap(Values.decodeSet).map(Values.encode(_, of)))
    case AtomicType.Map =>
      val ofValues = DataType(kind.args.last)
      val valuesByKey = Seqs.index(values.flatMap(Values.decodeMap))
      val mergedMap = valuesByKey.map { case (k, vs) =>
        k -> Values.decode(aggregate(ofValues, vs.map(Values.encode(_, ofValues))), ofValues)
      }
      Values.encodeMap(mergedMap, kind)
    case _ => throw new IllegalArgumentException(s"Cannot aggregate $kind values")
  }
}

/**
 * A list of metrics.
 *
 * @param runs
 * @param params
 * @param groups
 */
case class MetricList(runs: Seq[Run], params: Map[String, Value], groups: Seq[MetricGroup]) {
  /**
   * Filter the metrics being aggregated to only include those specified.
   *
   * @param names List of metric names.
   * @return A copy including only specified metrics.
   */
  def filter(names: Set[String]): MetricList = {
    if (names.isEmpty) {
      this
    } else if (names.contains("ALL")) {
      this
    } else {
      val newGroups = groups.filter(group => names.contains(group.name))
      copy(groups = newGroups)
    }
  }

  /**
   * Split this list of metrics into several lists, one per unique workflow parametrization. If this instance has
   * already been split, it will result in a singleton composed of itself.
   */
  def split: Seq[MetricList] = {
    runs
      .groupBy(_.params)
      .map { case (someParams, someRuns) =>
        val runIds = someRuns.map(_.id)
        val newGroups = groups.map { group =>
          val newValues = group.values.filter { case (runId, _) => runIds.contains(runId) }
          group.copy(values = newValues)
        }
        MetricList(someRuns, someParams.toMap, newGroups)
      }.toSeq
  }
}

/**
 * A group of metrics.
 *
 * @param name   Metric name.
 * @param values Metric values, keyed by run id.
 */
case class MetricGroup(name: String, values: Map[RunId, Double]) {
  val nodeName: String = name.take(name.indexOf("/"))

  val metricName: String = name.drop(name.indexOf("/") + 1)

  def toSeq: Seq[Metric] = values.values.map(v => Metric(metricName, v)).toSeq

  def aggregated: Metric = Metric(metricName, mean(values.values.toSeq))
}