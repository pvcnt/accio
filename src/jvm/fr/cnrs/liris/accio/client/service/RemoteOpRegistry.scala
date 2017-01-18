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

package fr.cnrs.liris.accio.client.service

import com.google.inject.Inject
import com.twitter.util.{Await, Return, Throw}
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.core.domain.{OpDef, OpRegistry}
import fr.cnrs.liris.accio.core.service.handler.ListOperatorsRequest

class RemoteOpRegistry @Inject()(client: AgentService.FinagledClient) extends OpRegistry with LazyLogging {
  private[this] lazy val index: Map[String, OpDef] = {
    val f = client.listOperators(ListOperatorsRequest(includeDeprecated = true)).liftToTry
    Await.result(f) match {
      case Return(resp) => resp.results.map(opDef => opDef.name -> opDef).toMap
      case Throw(e) =>
        logger.error("Error while retrieving operators", e)
        Map.empty[String, OpDef]
    }
  }

  override def ops: Set[OpDef] = index.values.toSet

  override def contains(name: String): Boolean = index.contains(name)

  override def get(name: String): Option[OpDef] = index.get(name)

  @throws[NoSuchElementException]
  override def apply(name: String): OpDef = index(name)
}
