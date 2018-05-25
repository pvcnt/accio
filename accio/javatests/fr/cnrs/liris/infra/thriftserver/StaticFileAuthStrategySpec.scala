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

package fr.cnrs.liris.infra.thriftserver

import com.twitter.util.Await
import fr.cnrs.liris.testing.{CreateTmpDirectory, UnitSpec}

/**
 * Unit tests for [[StaticFileAuthStrategy]].
 */
class StaticFileAuthStrategySpec extends UnitSpec with CreateTmpDirectory {
  behavior of "StaticAuthStrategy"

  private lazy val authenticator = {
    val path = copyResource("fr/cnrs/liris/infra/thriftserver/static_file.txt")
    StaticFileAuthStrategy.fromFile(path)
  }

  it should "allow a valid client identifier" in {
    Await.result(authenticator.authenticate("sometoken")) shouldBe Some(UserInfo("john"))
    Await.result(authenticator.authenticate("token")) shouldBe Some(UserInfo("jess", Some("jess@email.com")))
    Await.result(authenticator.authenticate("othertoken")) shouldBe Some(UserInfo("mark", Some("mark@email.com"), Set("foogroup")))
    Await.result(authenticator.authenticate("atoken")) shouldBe Some(UserInfo("anna", Some("anna@email.com"), Set("foogroup", "bargroup")))
    Await.result(authenticator.authenticate("anothertoken")) shouldBe Some(UserInfo("jack", None, Set("bargroup")))
  }

  it should "deny an invalid token" in {
    Await.result(authenticator.authenticate("unknowntoken")) shouldBe None
  }
}