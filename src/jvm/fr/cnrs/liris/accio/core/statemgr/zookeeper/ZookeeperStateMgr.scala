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

package fr.cnrs.liris.accio.core.statemgr.zookeeper

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.core.statemgr.{InjectStateMgr, StateManager}
import org.apache.curator.framework.CuratorFramework

/**
 * State manager storing data into a Zookeeper cluster.
 *
 * @param client Curator framework.
 * @param config Configuration.
 */
@Singleton
final class ZookeeperStateMgr @Inject()(@InjectStateMgr client: CuratorFramework, config: ZookeeperStateMgrConfig)
  extends StateManager {
  override def close(): Unit = {
    client.close()
  }
}