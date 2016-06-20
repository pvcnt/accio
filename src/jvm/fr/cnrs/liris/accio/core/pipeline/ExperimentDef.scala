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

import fr.cnrs.liris.accio.core.param.{ParamGrid, ParamMap}

case class User(name: String, email: Option[String] = None)

object User {
  private[this] val UserRegex = "(.+)<(.+)>".r

  def parse(str: String): User = str match {
    case UserRegex(name, email) => new User(name.trim, Some(email.trim))
    case _ => throw new IllegalArgumentException(s"Invalid user format '$str'")
  }
}

case class WorkflowDef(id: String, version: String, graph: GraphDef, name: Option[String], owner: Option[User]) {
  def setParams(paramMap: ParamMap): WorkflowDef = copy(graph = graph.setParams(paramMap))

  def setRuns(runs: Int): WorkflowDef = copy(graph = graph.setRuns(runs))
}

/**
 * Definition of an experiment. An experiment is based on a workflow. It can be a simple execution
 * of this workflow, an exploration of parameters or an optimization of parameters.
 *
 * @param name         Experiment name
 * @param workflow     Base workflow
 * @param paramMap     Override parameters map
 * @param exploration  Parameters exploration (cannot be combined with `optimization`)
 * @param optimization Parameters optimization (cannot be combined with `exploration`)
 * @param notes        Some notes
 * @param tags         Some tags
 * @param initiator    User initiating the experiment
 */
case class ExperimentDef(
    name: String,
    workflow: WorkflowDef,
    paramMap: Option[ParamMap],
    exploration: Option[Exploration],
    optimization: Option[Optimization],
    notes: Option[String],
    tags: Set[String],
    initiator: User) {
  require(!(exploration.isDefined && optimization.isDefined), "Cannot define both an exploration and an optimization")
}

case class Exploration(paramGrid: ParamGrid)

case class Optimization(
    paramGrid: ParamGrid,
    iters: Int,
    contraction: Double,
    objectives: Set[Objective]) {
  require(iters > 0, s"Number of iterations per step must be strictly positive (got $iters)")
  require(contraction > 0 && contraction <= 1, s"Contraction factor must be in (0,1] (got $contraction)")
}