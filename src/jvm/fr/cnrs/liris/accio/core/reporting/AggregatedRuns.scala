/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.reporting

import breeze.stats._
import fr.cnrs.liris.accio.core.framework.{Artifact, DataType, Run, Values}
import fr.cnrs.liris.common.geo.Distance
import fr.cnrs.liris.common.util.Seqs
import org.joda.time.Duration

/**
 * Aggregate data about several runs.
 *
 * @param runs Non-empty list of runs to aggregate.
 */
class AggregatedRuns(val runs: Seq[Run]) {
  require(runs.nonEmpty, "You must provide some runs to aggregate")

  /**
   * Filter the runs being aggregated to only include those specified. The list of identifiers specified to filter
   * can include both run or experiment identifiers. If the list is empty, no filtering will actually be applied.
   *
   * @param ids List of experiment or run identifiers.
   * @return A copy including only specified runs.
   */
  def filter(ids: Set[String]): AggregatedRuns = {
    if (ids.isEmpty) {
      this
    } else {
      val newRuns = runs.filter(run => ids.contains(run.id) || ids.contains(run.parent))
      new AggregatedRuns(newRuns)
    }
  }

  /**
   * Return an aggregated description of the artifacts contained inside those runs.
   */
  def artifacts: ArtifactList = {
    // Group all artifacts by run id.
    val artifactsByRun = runs.flatMap { run =>
      run.report
        .map(_.artifacts.map(art => run.id -> art))
        .getOrElse(Seq.empty[(String, Artifact)])
    }

    // Group artifacts by their name, and then by run id..
    val groups = artifactsByRun
      .groupBy(_._2.name)
      .map { case (name, list) =>
        val values = list.map { case (runId, art) => runId -> art.value }
        ArtifactGroup(name, list.head._2.kind, values.toMap)
      }.toSeq

    // Only keep runs that matter.
    val runIds = artifactsByRun.map(_._1).toSet
    val effectiveRuns = runs.filter(run => runIds.contains(run.id))

    ArtifactList(effectiveRuns, Map.empty, groups)
  }
}

/**
 * A list of artifacts.
 *
 * @param runs
 * @param params
 * @param groups
 */
case class ArtifactList(runs: Seq[Run], params: Map[String, Any], groups: Seq[ArtifactGroup]) {
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
    } else {
      val includeNumeric = names.contains("NUMERIC")
      val includeAll = names.contains("ALL")
      val newGroups = groups.filter { group =>
        includeAll || names.contains(group.name) || (includeNumeric && group.kind.isNumeric)
      }
      copy(groups = newGroups)
    }
  }

  /**
   * Split this list of artifacts into several lists, one per unique workflow parametrization. If this instance has
   * already been split, it will result in a singleton composed of itself.
   */
  def split: Seq[ArtifactList] = {
    runs
      .groupBy(_.params)
      .map { case (someParams, someRuns) =>
        val runIds = someRuns.map(_.id)
        val newGroups = groups.map { group =>
          val newValues = group.values.filter { case (runId, _) => runIds.contains(runId) }
          group.copy(values = newValues)
        }
        ArtifactList(someRuns, someParams, newGroups)
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
case class ArtifactGroup(name: String, kind: DataType, values: Map[String, Any]) {
  def size: Int = values.size

  def isEmpty: Boolean = values.isEmpty

  def nonEmpty: Boolean = values.nonEmpty

  def toSeq: Seq[Artifact] = values.values.map(v => Artifact(name, kind, v)).toSeq

  def aggregated: Artifact = Artifact(name, kind, aggregate(kind, values.values.toSeq))

  private def aggregate(kind: DataType, values: Seq[Any]): Any = kind match {
    case DataType.Byte => mean(values.map(Values.asDouble))
    case DataType.Short => mean(values.map(Values.asDouble))
    case DataType.Integer => mean(values.map(Values.asDouble))
    case DataType.Double => mean(values.map(Values.asDouble))
    case DataType.Long => mean(values.map(Values.asDouble))
    case DataType.Distance => Distance.meters(mean(values.map(Values.asDistance(_).meters)))
    case DataType.Duration => Duration.millis(mean(values.map(Values.asDuration(_).getMillis.toDouble)).round)
    case DataType.List(of) => aggregate(of, values.flatMap(Values.asList(_, of)))
    case DataType.Set(of) => aggregate(of, values.flatMap(Values.asSet(_, of)))
    case DataType.Map(ofKeys, ofValues) =>
      val valuesByKey = Seqs.index(values.flatMap(Values.asMap(_, ofKeys, ofValues)))
      valuesByKey.map { case (k, vs) => k -> aggregate(ofValues, vs) }
    case _ => throw new IllegalArgumentException(s"Cannot aggregate $kind values")
  }
}