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

import fr.cnrs.liris.accio.core.statemgr.StateMgrSpec
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryOneTime
import org.apache.curator.test.TestingServer
import org.scalatest.BeforeAndAfterEach

/**
 * Unit tests of [[ZookeeperStateMgr]].
 */
class ZookeeperStateMgrSpec extends StateMgrSpec with BeforeAndAfterEach {
  private[this] var zkTestServer: TestingServer = null
  private[this] var client: CuratorFramework = null

  override protected def beforeEach(): Unit = {
    zkTestServer = new TestingServer(2181)
    client = CuratorFrameworkFactory.newClient(zkTestServer.getConnectString, new RetryOneTime(2000))
    client.start()
  }

  override protected def afterEach(): Unit = {
    client.close()
    zkTestServer.stop()
  }

  protected def createStateMgr = new ZookeeperStateMgr(client, "/accio")

  behavior of "ZookeeperStateMgr"
}