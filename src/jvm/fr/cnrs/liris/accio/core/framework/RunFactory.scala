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

package fr.cnrs.liris.accio.core.framework

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.common.util.{HashUtils, Seqs}

import scala.util.Random

/**
 * Factory for [[Run]]s.
 *
 * @param opRegistry Operator registry.
 */
@Singleton
final class RunFactory @Inject()(opRegistry: OpRegistry) {
  /**
   * Create a list of runs for a given experiment.
   *
   * @param experiment Experiment.
   */
  def create(experiment: Experiment): Seq[Run] = {
    val rnd = new Random(experiment.seed)
    expand(experiment).map { case (name, graph) =>
      val runId = HashUtils.sha1(UUID.randomUUID().toString)
      val seed = rnd.nextLong()
      Run(id = runId, parent = experiment.id, graph = graph, name = Some(name), seed = seed)
    }
  }

  private def expand(experiment: Experiment): Seq[(String, Graph)] = {
    expandParams(experiment.params, experiment.name, experiment.workflow.graph)
      .flatMap { case (name, graph) =>
        expandRuns(experiment.runs, name, graph)
      }
  }

  private def expandParams(params: Map[Reference, Exploration], name: String, graph: Graph): Seq[(String, Graph)] = {
    if (params.nonEmpty) {
      val allValues = params.map { case (ref, explo) =>
        val argDef = opRegistry(graph(ref.node).op).defn.inputs.find(_.name == ref.port).get
        Values.expand(explo, argDef.kind).map(v => ref -> v)
      }.toSeq
      Seqs.crossProduct(allValues).map { params =>
        val runName = name + ":" + params.map { case (k, v) => s"$k=$v" }.mkString(",")
        (runName, graph.setParams(params.toMap))
      }
    } else {
      Seq((name, graph))
    }
  }

  private def expandRuns(repeat: Int, name: String, graph: Graph): Seq[(String, Graph)] = {
    Seq.tabulate(repeat)(idx => (s"$name#${idx + 1}", graph))
  }
}