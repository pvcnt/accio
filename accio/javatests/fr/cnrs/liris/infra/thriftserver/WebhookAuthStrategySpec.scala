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

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util.{Await, Duration, Future}
import fr.cnrs.liris.infra.thriftserver.WebhookAuthStrategy.ReviewResponse
import fr.cnrs.liris.infra.webhook.Webhook
import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfter

/**
 * Unit tests for [[WebhookAuthStrategy]].
 */
class WebhookAuthStrategySpec extends UnitSpec with BeforeAndAfter {
  behavior of "WebhookAuthStrategy"

  it should "allow a valid client identifier" in {
    val service = new Service[Request, Response] {
      override def apply(request: Request): Future[Response] = {
        val resp = Response(Status.Ok)
        resp.setContentTypeJson()
        resp.contentString = """{"authenticated":true,"user":{"name":"john","email":"john@email.com","groups":["foo","bar"]}}"""
        Future.value(resp)
      }
    }
    val strategy = new WebhookAuthStrategy(new Webhook[ReviewResponse](service, "/", Map.empty), Duration.Zero)
    Await.result(strategy.authenticate("anytoken")) shouldBe Some(UserInfo("john", Some("john@email.com"), Set("foo", "bar")))
  }

  it should "deny an invalid token" in {
    val service = new Service[Request, Response] {
      override def apply(request: Request): Future[Response] = {
        val resp = Response(Status.Ok)
        resp.setContentTypeJson()
        // It should fail even if the user is provided (which should not be the case...).
        resp.contentString = """{"authenticated":false,"user":{"name":"john"}}"""
        Future.value(resp)
      }
    }
    val strategy = new WebhookAuthStrategy(new Webhook[ReviewResponse](service, "/", Map.empty), Duration.Zero)
    Await.result(strategy.authenticate("anytoken")) shouldBe None
  }

  it should "deny if the service returns an unexpected HTTP code" in {
    val service = new Service[Request, Response] {
      override def apply(request: Request): Future[Response] = Future.value(Response(Status.NotFound))
    }
    val strategy = new WebhookAuthStrategy(new Webhook[ReviewResponse](service, "/", Map.empty), Duration.Zero)
    Await.result(strategy.authenticate("anytoken")) shouldBe None
  }

  it should "deny if the service returns an invalid response" in {
    val service = new Service[Request, Response] {
      override def apply(request: Request): Future[Response] = {
        val resp = Response(Status.Ok)
        resp.setContentTypeJson()
        resp.contentString = """{"foo": "bar"}"""
        Future.value(resp)
      }
    }
    val strategy = new WebhookAuthStrategy(new Webhook[ReviewResponse](service, "/", Map.empty), Duration.Zero)
    Await.result(strategy.authenticate("anytoken")) shouldBe None
  }
}