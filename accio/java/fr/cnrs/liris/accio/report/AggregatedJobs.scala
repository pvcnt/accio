/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.accio.report

import fr.cnrs.liris.accio.validation.thrift._
import fr.cnrs.liris.accio.validation.{DataTypes, Values}
import fr.cnrs.liris.util.MathUtils.mean
import fr.cnrs.liris.util.Seqs
import fr.cnrs.liris.util.geo.Distance
import org.joda.time.Duration

/**
 * Aggregate data about several jobs.
 *
 * @param jobs Non-empty list of jobs to aggregate.
 */
final class AggregatedJobs(val jobs: Seq[Job]) {
  require(jobs.nonEmpty, "You must provide some jobs to aggregate")

  def size: Int = jobs.size

  /**
   * Return an aggregated description of the artifacts contained inside those jobs.
   */
  def artifacts: ArtifactList = {
    // Group all artifacts by run id.
    val artifactsByRun = jobs.flatMap { job =>
      job.status.artifacts.toSeq.flatten.map(art => job.name -> art)
    }

    // Group artifacts by their name, and then by run id..
    val groups = artifactsByRun
      .groupBy(_._2.name)
      .map { case (artifactName, list) =>
        val values = list.map { case (runId, art) => runId -> art.value }
        ArtifactGroup(artifactName, list.head._2.value.dataType, values.toMap)
      }.toSeq

    // Only keep runs for which we have at least one artifact.
    val runIds = artifactsByRun.map(_._1).toSet
    val effectiveRuns = jobs.filter(run => runIds.contains(run.name))

    ArtifactList(effectiveRuns, Seq.empty, groups)
  }
}

/**
 * A list of artifacts.
 *
 * @param jobs
 * @param params
 * @param groups
 */
case class ArtifactList(jobs: Seq[Job], params: Seq[NamedValue], groups: Seq[ArtifactGroup]) {
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
   * Split this list of artifacts into several lists, one per unique workflow parametrization. If
   * this instance has already been split, it will result in a singleton composed of itself.
   */
  def split: Seq[ArtifactList] = {
    if (params.nonEmpty) {
      Seq(this)
    } else {
      val commonParams = Seqs.index(jobs.flatMap(_.params).map(param => param.name -> param))
        .filter { case (_, params) => params.toSet.size == 1 }
        .keySet
      val runsByParams = jobs.groupBy(_.params)
      runsByParams.map { case (someParams, someRuns) =>
        val runIds = someRuns.map(_.name)
        val newGroups = groups.map { group =>
          val newValues = group.values.filter { case (runId, _) => runIds.contains(runId) }
          group.copy(values = newValues)
        }
        ArtifactList(someRuns, someParams.filter(param => !commonParams.contains(param.name)), newGroups)
      }.toSeq
    }
  }
}

/**
 * A group of artifacts.
 *
 * @param name   Artifact name.
 * @param kind   Artifact data type.
 * @param values Artifact values, keyed by run id.
 */
case class ArtifactGroup(name: String, kind: DataType, values: Map[String, Value]) {
  def toSeq: Seq[NamedValue] = values.values.map(v => NamedValue(name, v)).toSeq

  def aggregated: NamedValue = NamedValue(name, aggregate(kind, values.values.toSeq))

  private def aggregate(kind: DataType, values: Seq[Value]): Value =
    kind match {
      case DataType.Atomic(tpe) => aggregate(tpe, values)
      case DataType.ListType(tpe) =>
        aggregate(
          tpe.values,
          values
            .flatMap(Values.decodeList(_, tpe))
            .flatMap(Values.encode(_, DataType.Atomic(tpe.values))))
      case DataType.MapType(tpe) =>
        val valuesByKey = Seqs.index(values.flatMap(Values.decodeMap(_, tpe)))
        val mergedMap = valuesByKey.map { case (k, vs) =>
          k -> Values.decode(aggregate(tpe.values, vs.flatMap(Values.encode(_, DataType.Atomic(tpe.values)))), DataType.Atomic(tpe.values))
        }
        Values
          .encodeMap(mergedMap, tpe)
          .getOrElse(throw new IllegalArgumentException(s"Cannot aggregate $kind values"))
      case _ => throw new IllegalArgumentException(s"Cannot aggregate $kind values")
    }

  private def aggregate(kind: AtomicType, values: Seq[Value]): Value =
    kind match {
      case AtomicType.Integer => Values.encodeDouble(mean(values.map(Values.decodeInteger(_).toDouble)))
      case AtomicType.Float => Values.encodeFloat(mean(values.map(Values.decodeFloat(_).toDouble)).toFloat)
      case AtomicType.Double => Values.encodeDouble(mean(values.map(Values.decodeDouble)))
      case AtomicType.Long => Values.encodeDouble(mean(values.map(Values.decodeLong(_).toDouble)))
      case AtomicType.Distance =>
        Values.encodeDistance(Distance.meters(mean(values.map(Values.decodeDistance(_).meters))))
      case AtomicType.Duration =>
        Values.encodeDuration(Duration.millis(mean(values.map(Values.decodeDuration(_).getMillis.toDouble)).round))
      case _ => throw new IllegalArgumentException(s"Cannot aggregate $kind values")
    }
}