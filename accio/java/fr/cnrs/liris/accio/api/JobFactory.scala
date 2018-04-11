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

package fr.cnrs.liris.accio.api

import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.util.Seqs

import scala.util.Random

/**
 * Job factory.
 */
final class JobFactory {
  /**
   * Create one or several jobs from a job. At least one job is guaranteed to be returned. If
   * several jobs are returned, the first one is the parent job and the other ones are children
   * jobs.
   *
   * @param job Job.
   * @return List of jobs.
   */
  def create(job: Job): Seq[Job] = {
    // Expand parameters w.r.t. to parameter sweep.
    val expandedParams = expandParams(job.params)

    // Create all jobs.
    if (expandedParams.size == 1) {
      Seq(createSingle(job))
    } else {
      createSweep(job, expandedParams)
    }
  }

  private def createSingle(job: Job): Job = {
    val tasks = job.steps.map(step => Task(step.name))
    job.copy(status = JobStatus(tasks = Some(tasks)))
  }

  private def createSweep(job: Job, expandedParams: Seq[Seq[NamedValue]]): Seq[Job] = {
    val fixedParams = job.params.groupBy(_.name).values.filter(_.size == 1).map(_.head.name).toSet
    val random = new Random(job.seed)

    // Impl. note: As of now, with Scala 2.11.8, I have to explicitly write the type of `children` to be Seq[Run].
    // If I don't force it, I get the strangest compiler error (head lines follow):
    // java.lang.AssertionError: assertion failed:
    //   Some(children.<map: error>(((x$2) => x$2.id)).<toSet: error>)
    //     while compiling: /path/to/code/accio/java/fr/cnrs/liris/accio/api/JobFactory.scala
    //       during phase: superaccessors
    val children: Seq[Job] = expandedParams.zipWithIndex.map { case (params, idx) =>
      val discriminantParams = params.filter(param => !fixedParams.contains(param.name))
      val title = if (discriminantParams.nonEmpty) Utils.label(discriminantParams) else s"Job #$idx"
      createSingle(job.copy(
        name = s"${job.name}.$idx",
        title = Some(title),
        params = params,
        seed = random.nextLong(),
        parent = Some(job.name)))
    }

    val parent = job.copy(status = JobStatus(children = Some(Map(ExecState.Pending -> children.size))))

    Seq(parent) ++ children
  }

  private def expandParams(params: Seq[NamedValue]): Seq[Seq[NamedValue]] = {
    if (params.nonEmpty) {
      val groupedParams = params.groupBy(_.name).values.toSeq
      Seqs.crossProduct(groupedParams)
    } else {
      Seq.empty
    }
  }
}