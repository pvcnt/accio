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
import fr.cnrs.liris.accio.agent.{GetDatasetRequest, GetDatasetResponse}
import fr.cnrs.liris.accio.core.storage.Storage
import fr.cnrs.liris.accio.runtime.commandbus.AbstractHandler
import fr.cnrs.liris.dal.core.io.Decoder

final class GetDatasetHandler @Inject()(storage: Storage, decoders: Set[Decoder[_]])
  extends AbstractHandler[GetDatasetRequest, GetDatasetResponse] {

  override def handle(req: GetDatasetRequest): Future[GetDatasetResponse] = {
    Future.value(GetDatasetResponse(Seq.empty, 0))
    /*val maybeArtifact = storage.read(_.runs.get(req.runId))
          .flatMap(_.state.nodes.find(_.name == req.nodeName))
          .flatMap(_.result.flatMap(_.artifacts.find(_.name == req.portName)))
    maybeArtifact match {
      case None => Future.value(GetDatasetResponse(Seq.empty, 0))
      case Some(artifact) =>
        artifact.value.kind match {
          case DataType(AtomicType.Dataset, _) =>

          case _ => Future.value(GetDatasetResponse(Seq.empty, 0))
        }
    }*/
  }
}
