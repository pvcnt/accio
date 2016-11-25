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
    expandParams(experiment.params, experiment.name, experiment.workflow)
      .flatMap { case (name, params) =>
        expandRuns(experiment.repeat, experiment.seed, name, params)
      }
      .map { case (name, params, seed) =>
        val runId = HashUtils.sha1(UUID.randomUUID().toString)
        Run(id = runId, parent = experiment.id, name = name, seed = seed, params = params)
      }
  }

  /**
   * Expand an experiment to take into account the space of parameters being explored.
   *
   * @param params   Experiment parameters.
   * @param name     Experiment name.
   * @param workflow Workflow.
   * @return List of (run name, run parameters).
   */
  private def expandParams(params: Map[String, Exploration], name: String, workflow: Workflow): Seq[(String, Map[String, Any])] = {
    if (params.nonEmpty) {
      val allValues = params.map { case (paramName, explo) =>
        // We are guaranteed that the param exists, because of the experiment construction.
        val param = workflow.params.find(_.name == paramName).get
        explo.expand(param.kind).map(v => (paramName, v)).toSeq
      }.toSeq
      Seqs.crossProduct(allValues).map { params =>
        val label = Run.label(params)
        (s"$name:$label", params.toMap)
      }
    } else {
      Seq((name, Map.empty[String, Any]))
    }
  }

  /**
   * Expand runs to take into account number of times the experiment should be repeated.
   *
   * @param repeat Number of times to repeat the experiment.
   * @param seed   Experiment's seed.
   * @param name   Run name.
   * @param params Run parameters.
   * @return List of (run name, run parameters, run seed).
   */
  private def expandRuns(repeat: Int, seed: Long, name: String, params: Map[String, Any]): Seq[(String, Map[String, Any], Long)] = {
    if (repeat <= 1) {
      Seq((name, params, seed))
    } else {
      val rnd = new Random(seed)
      Seq.tabulate(repeat)(idx => (s"$name#${idx + 1}", params, rnd.nextLong()))
    }
  }
}