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

import java.util.concurrent.TimeUnit

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.core.statemgr.{InjectStateMgr, Lock, StateManager}
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.locks.InterProcessMutex

/**
 * State manager storing data into a Zookeeper cluster.
 *
 * @param client Curator framework.
 * @param config Configuration.
 */
@Singleton
final class ZookeeperStateMgr @Inject()(@InjectStateMgr client: CuratorFramework, config: ZookeeperStateMgrConfig)
  extends StateManager {
  //private[this] val protocolFactory = new TBinaryProtocol.Factory()

  override def lock(key: String): Lock = new ZookeeperLock(key)

  override def close(): Unit = {
    client.close()
  }

  /*override def get(key: String): Option[Array[Byte]] = {
    try {
      Some(client.getData.forPath(dataPath(key)))
    } catch {
      case e: KeeperException =>
        // A KeeperException is thrown if the node does not exist.
        if (e.code != KeeperException.Code.NONODE) {
          throw e
        }
        None
    }
  }

  override def set(key: String, value: Array[Byte]): Unit = {
    client.create().orSetData().creatingParentsIfNeeded().forPath(dataPath(key), value)
  }

  override def list(key: String): Set[String] =
    client.getChildren.forPath(dataPath(key))
      .asScala
      .toSet
      .map((subKey: String) => s"$key/$subKey")*/

  private def locksPath = s"${config.prefix}/locks"

  //private def dataPath(key: String) = s"${config.prefix}/data/$key"

  private class ZookeeperLock(key: String) extends Lock {
    private[this] val zkLock = new InterProcessMutex(client, s"$locksPath/$key")

    override def lock(): Unit = zkLock.acquire()

    override def tryLock(): Boolean = zkLock.acquire(10, TimeUnit.MILLISECONDS)

    override def unlock(): Unit = zkLock.release()
  }

}