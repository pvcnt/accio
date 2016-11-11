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
    expand(experiment).map { case (name, params) =>
      val runId = HashUtils.sha1(UUID.randomUUID().toString)
      val seed = rnd.nextLong()
      Run(id = runId, parent = experiment.id, name = name, seed = seed, params = params)
    }
  }

  private def expand(experiment: Experiment): Seq[(String, Map[String, Any])] = {
    expandParams(experiment.params, experiment.name, experiment.workflow)
      .flatMap { case (name, params) => expandRuns(experiment.repeat, name, params) }
  }

  private def expandParams(params: Map[String, Exploration], name: String, workflow: Workflow): Seq[(String, Map[String, Any])] = {
    if (params.nonEmpty) {
      val allValues = params.map { case (paramName, explo) =>
        // We are guaranteed that the param exist, because of the workflow construction.
        val param = workflow.params.find(_.name == paramName).get
        //TODO: take into account logarithmic progressions.
        Values.expand(explo, param.kind).map(v => (paramName, v))
      }.toSeq
      Seqs.crossProduct(allValues).map { params =>
        val runName = name + ":" + params.map { case (k, v) => s"$k=$v" }.mkString(",")
        (runName, params.toMap)
      }
    } else {
      Seq((name, Map.empty[String, Any]))
    }
  }

  private def expandRuns(repeat: Int, name: String, params: Map[String, Any]): Seq[(String, Map[String, Any])] = {
    if (repeat <= 1) {
      Seq((name, params))
    } else {
      Seq.tabulate(repeat)(idx => (s"$name#${idx + 1}", params))
    }
  }
}