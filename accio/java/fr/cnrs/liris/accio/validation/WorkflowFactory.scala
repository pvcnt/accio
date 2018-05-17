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

package fr.cnrs.liris.accio.validation

import fr.cnrs.liris.accio.domain.Workflow
import fr.cnrs.liris.lumos.domain.AttrValue

import scala.util.Random

/**
 * Workflow factory.
 */
final class WorkflowFactory {
  /**
   * Create one or several workflows from a user-specified workflow. At least one workflow is
   * guaranteed to be returned. If several jobs are returned, the first one is the parent job and
   * the other ones are children jobs.
   *
   * @param workflow Initial workflow.
   * @return List of workflows.
   */
  def create(workflow: Workflow): Seq[Workflow] = {
    // Expand parameters w.r.t. to parameter sweep.
    val expandedParams = expandParams(workflow.params)

    // Create all workflows.
    if (expandedParams.size == 1) {
      Seq(workflow)
    } else {
      createSweep(workflow, expandedParams)
    }
  }

  private def createSweep(workflow: Workflow, expandedParams: Seq[Seq[AttrValue]]): Seq[Workflow] = {
    //val fixedParams = workflow.params.groupBy(_.name).values.filter(_.size == 1).map(_.head.name).toSet
    val random = new Random(workflow.seed)

    // Impl. note: As of now, with Scala 2.11.8, I have to explicitly write the type of `children` to be Seq[Workflow].
    // If I don't force it, I get the strangest compiler error (head lines follow):
    // java.lang.AssertionError: assertion failed:
    //   Some(children.<map: error>(((x$2) => x$2.id)).<toSet: error>)
    //     while compiling: /path/to/code/accio/java/fr/cnrs/liris/accio/domain/WorkflowFactory.scala
    //       during phase: superaccessors
    expandedParams.zipWithIndex.map { case (params, idx) =>
      //val discriminantParams = params.filter(param => !fixedParams.contains(param.name))
      //val title = if (discriminantParams.nonEmpty) Utils.label(discriminantParams) else s"Job #$idx"
      workflow.copy(
        name = s"${workflow.name}.$idx",
        params = params,
        seed = random.nextLong(),
        repeat = 1,
        labels = workflow.labels ++ Set("parent" -> workflow.name))
    }
  }

  private def expandParams(params: Seq[AttrValue]): Seq[Seq[AttrValue]] = {
    if (params.nonEmpty) {
      val groupedParams = params.groupBy(_.name).values.toSeq
      crossProduct(groupedParams)
    } else {
      Seq.empty
    }
  }

  /**
   * Generate a cross product between several lists, i.e., all possible combinations between
   * their values.
   *
   * @param input Lists of node inputs between which to perform the cross product
   * @return Cross product of these lists
   */
  private def crossProduct[T](input: Seq[Seq[T]]): Seq[Seq[T]] = {
    //https://stackoverflow.com/questions/13567543/cross-product-of-arbitrary-number-of-lists-in-scala
    val zss: Seq[Seq[T]] = Seq(Seq())

    def fun(xs: Seq[T], zss: Seq[Seq[T]]): Seq[Seq[T]] = {
      for {
        x <- xs
        zs <- zss
      } yield Seq[T](x) ++ zs
    }

    input.foldRight(zss)(fun)
  }
}