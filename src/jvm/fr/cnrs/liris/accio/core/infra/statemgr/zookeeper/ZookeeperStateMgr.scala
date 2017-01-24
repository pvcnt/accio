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

package fr.cnrs.liris.accio.core.infra.statemgr.zookeeper

import java.util.concurrent.TimeUnit

import com.twitter.scrooge.TArrayByteTransport
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.service.{Lock, StateManager}
import fr.cnrs.liris.accio.core.domain.{Task, TaskId}
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.locks.InterProcessMutex
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.zookeeper.KeeperException

import scala.collection.JavaConverters._

/**
 * State manager storing data into a Zookeeper cluster.
 *
 * @param client   Curator framework.
 * @param rootPath Root path under which to store data.
 */
final class ZookeeperStateMgr(client: CuratorFramework, rootPath: String)
  extends StateManager with StrictLogging {

  private[this] val protocolFactory = new TBinaryProtocol.Factory()

  override def lock(key: String): Lock = new ZookeeperLock(key)

  override def tasks: Set[Task] = {
    client.getChildren.forPath(tasksPath).asScala.toSet.flatMap((nodeName: String) => get(TaskId(nodeName)))
  }

  override def save(task: Task): Unit = {
    val transport = new TArrayByteTransport
    val protocol = protocolFactory.getProtocol(transport)
    task.write(protocol)
    val bytes = transport.toByteArray
    client.create().orSetData().creatingParentsIfNeeded().forPath(taskPath(task.id), bytes)
    logger.debug(s"Saved task ${task.id.value}")
  }

  override def remove(id: TaskId): Unit = {
    client.delete().forPath(taskPath(id))
    logger.debug(s"Removed task ${id.value}")
  }

  override def get(id: TaskId): Option[Task] = {
    try {
      val bytes = client.getData.forPath(taskPath(id))
      val transport = TArrayByteTransport(bytes)
      val protocol = protocolFactory.getProtocol(transport)
      Some(Task.decode(protocol))
    } catch {
      case e: KeeperException =>
        // A KeeperException is thrown if the node does not exist.
        if (e.code != KeeperException.Code.NONODE) {
          throw e
        }
        None
    }
  }

  private def tasksPath = s"$rootPath/tasks"

  private def locksPath = s"$rootPath/locks"

  private def taskPath(id: TaskId) = s"$tasksPath/${id.value}"

  private class ZookeeperLock(key: String) extends Lock {
    private[this] val zkLock = new InterProcessMutex(client, s"$locksPath/$key")

    override def lock(): Unit = {
      logger.trace(s"Acquiring lock on $key")
      zkLock.acquire()
    }

    override def tryLock(): Boolean = {
      val res = zkLock.acquire(10, TimeUnit.MILLISECONDS)
      if (res) {
        logger.trace(s"Acquired lock on $key")
      }
      res
    }

    override def unlock(): Unit = {
      zkLock.release()
      logger.trace(s"Released lock on $key")
    }
  }

}