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

package fr.cnrs.liris.accio.agent.handler

import com.google.inject.Inject
import com.twitter.util.Future
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.agent.{InvalidTaskException, StreamLogsRequest, StreamLogsResponse}
import fr.cnrs.liris.accio.core.storage.MutableRunRepository

/**
 * Receive run logs from a task.
 *
 * @param runRepository Run repository.
 */
class StreamLogsHandler @Inject()(runRepository: MutableRunRepository)
  extends Handler[StreamLogsRequest, StreamLogsResponse] with LazyLogging {

  @throws[InvalidTaskException]
  override def handle(req: StreamLogsRequest): Future[StreamLogsResponse] = {
    // There is not need to lock here, as logs are append-only. We only take care of not inserting logs belonging
    // to an unknown run (that could have been killed or deleted in between).
    val runIds = req.logs.map(_.runId).toSet
    val unknownRunIds = runIds.filterNot(runRepository.contains)
    unknownRunIds.foreach { runId =>
      // We do not throw an exception because it can be a "normal" situation (if the thread sending logs is stopping).
      logger.warn(s"Received logs from unknown run ${runId.value}")
    }
    runRepository.save(req.logs.filterNot(log => unknownRunIds.contains(log.runId)))
    Future(StreamLogsResponse())
  }
}