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

import com.twitter.util.{Await, Future}
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[AuthChain]].
 */
class AuthenticatorChainSpec extends UnitSpec {
  behavior of "AuthChain"

  it should "authenticate a valid user" in {
    var authenticator = new AuthChain(Seq(FakeAuthenticator), allowAnonymous = false)
    Await.result(authenticator.authenticate(Some("backdoor"))) shouldBe Some(UserInfo("john", None, Set("system:authenticated")))

    authenticator = new AuthChain(Seq(DenyAuthenticator, AllowAuthenticator), allowAnonymous = false)
    Await.result(authenticator.authenticate(Some("backdoor"))) shouldBe Some(UserInfo("john", None, Set("system:authenticated")))
  }

  it should "reject an invalid user" in {
    val authenticator = new AuthChain(Seq(FakeAuthenticator), allowAnonymous = false)
    Await.result(authenticator.authenticate(Some("invalid"))) shouldBe None
    Await.result(authenticator.authenticate(None)) shouldBe None
  }

  it should "authenticate as anonymous" in {
    val authenticator = new AuthChain(Seq(FakeAuthenticator), allowAnonymous = true)
    Await.result(authenticator.authenticate(Some("invalid"))) shouldBe Some(UserInfo.Anonymous)
    Await.result(authenticator.authenticate(None)) shouldBe Some(UserInfo.Anonymous)
  }
}

private object FakeAuthenticator extends AuthStrategy {
  override def authenticate(credentials: String): Future[Option[UserInfo]] = {
    if (credentials == "backdoor") {
      Future.value(Some(UserInfo("john")))
    } else {
      Future.value(None)
    }
  }
}

private object AllowAuthenticator extends AuthStrategy {
  override def authenticate(credentials: String): Future[Option[UserInfo]] = Future.value(Some(UserInfo("john")))
}

private object DenyAuthenticator extends AuthStrategy {
  override def authenticate(credentials: String): Future[Option[UserInfo]] = Future.value(None)
}