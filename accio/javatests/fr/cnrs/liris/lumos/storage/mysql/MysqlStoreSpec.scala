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

package fr.cnrs.liris.lumos.storage.mysql

import java.util.UUID

import com.twitter.finagle.Mysql
import com.twitter.finagle.mysql.{Client => MysqlClient}
import com.twitter.util.Await
import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfterEach

private[mysql] trait MysqlStoreSpec extends UnitSpec with BeforeAndAfterEach {
  private[this] var initClient: MysqlClient = _
  private[this] var database: String = _

  override def beforeEach(): Unit = {
    database = "test_" + UUID.randomUUID().getLeastSignificantBits.toHexString
    initClient = Mysql.client.withCredentials(user, password).newRichClient(s"$host:3306")
    Await.result(initClient.query(s"create database $database"))
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    super.afterEach()
    Await.result(initClient.query(s"drop database $database"))
    initClient.close()
    initClient = null
    database = null
  }

  protected final def createClient = MysqlClientFactory(s"$host:3306", user, password, database)

  private final def host = sys.env.getOrElse("MYSQL_HOST", "0.0.0.0")

  private final def user = sys.env.getOrElse("MYSQL_USER", "root")

  private final def password = sys.env.get("MYSQL_PASSWORD").orNull
}