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

package fr.cnrs.liris.accio.agent.handler.api

import com.google.inject.Inject
import com.twitter.util.{Future, FuturePool}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent.commandbus.AbstractHandler
import fr.cnrs.liris.accio.agent.{CreateRunRequest, CreateRunResponse}
import fr.cnrs.liris.accio.core.domain.{InvalidSpecException, InvalidSpecMessage, RunId}
import fr.cnrs.liris.accio.core.framework.{RunFactory, RunManager}
import fr.cnrs.liris.accio.core.storage.MutableRunRepository
import fr.cnrs.liris.accio.core.util.WorkerPool

import scala.collection.mutable

/**
 * Launch a workflow, through one or several runs. It saves them and asynchronously schedules them.
 *
 * @param runFactory    Run factory.
 * @param runRepository Run repository.
 * @param runManager    Run lifecycle manager.
 * @param pool          Worker pool.
 */
final class CreateRunHandler @Inject()(
  runFactory: RunFactory,
  runRepository: MutableRunRepository,
  runManager: RunManager,
  @WorkerPool pool: FuturePool)
  extends AbstractHandler[CreateRunRequest, CreateRunResponse] with StrictLogging {

  @throws[InvalidSpecException]
  def handle(req: CreateRunRequest): Future[CreateRunResponse] = {
    val warnings = mutable.Set.empty[InvalidSpecMessage]
    val runs = runFactory.create(req.spec, req.user, warnings)
    runs.foreach(runRepository.save)

    // We save the runs before launching them asynchronously. It allows to return more quickly to the client, as
    // launching runs can take some times.
    if (runs.nonEmpty) {
      // Maybe it could happen that an unfortunate parameter sweep generate no run...
      pool(launch(runs.map(_.id)))
    }
    Future(CreateRunResponse(runs.map(_.id), warnings.toSeq))
  }

  /**
   * Launch a list of runs. Launching all runs is done as a batch (instead of independently for each run), as we may
   * need to update the parent run, and thus have to synchronize around it.
   *
   * @param ids Identifiers of the runs to launch. If several runs are provided, the first one is expected to be the
   *            parent and the other its children.
   */
  private def launch(ids: Seq[RunId]) = {
    // We do not reuse runs from the `handle` method, as they could have been modified in-between by other calls.
    // This is why we fetch them back from the repository.
    runRepository.get(ids.head).foreach { parent =>
      // We are guaranteed there will be at least one run. It is either a single run or the parent run, we may
      // launch it safely.
      var (newParent, _) = runManager.launch(parent, None)

      // Sequentially launch other runs, which are children of the first run.
      ids.tail.foreach { childId =>
        runRepository.get(childId).foreach { run =>
          val res = runManager.launch(run, Some(newParent))
          runRepository.save(res._1)
          res._2.foreach { nextParent =>
            newParent = nextParent
          }
        }
      }
      // We finally save the parent run.
      // TODO: maybe we should save it after each child run started.
      runRepository.save(newParent)
    }
  }
}