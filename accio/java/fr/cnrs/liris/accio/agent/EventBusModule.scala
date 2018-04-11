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

package fr.cnrs.liris.accio.agent

import java.util.concurrent.Executors

import com.google.common.eventbus._
import com.google.inject.{Provides, Singleton}
import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.logging.Logger
import org.slf4j.LoggerFactory

object EventBusModule extends TwitterModule {
  @Provides
  @Singleton
  def providesEventBus(statsReceiver: StatsReceiver): EventBus = {
    val logger = Logger(LoggerFactory.getLogger(classOf[EventBus].getName + ".default"))

    val deadEventCounter = statsReceiver.counter("eventbus", "default", "dead_events")
    val deadEventHandler = new {
      @Subscribe
      def onDeadEvent(event: DeadEvent): Unit = {
        deadEventCounter.incr()
        logger.warn(s"Captured dead event ${event.getEvent}")
      }
    }

    val exceptionCounter = statsReceiver.counter("eventbus", "default", "exceptions")
    val exceptionHandler = new SubscriberExceptionHandler {
      override def handleException(e: Throwable, context: SubscriberExceptionContext): Unit = {
        exceptionCounter.incr()
        if (logger.isErrorEnabled) {
          logger.error(
            s"Failed to dispatch ${context.getEvent.getClass.getName} event to " +
              s"${context.getSubscriberMethod}: $e",
            e)
        }
      }
    }

    val executor = Executors.newCachedThreadPool(new NamedPoolThreadFactory("eventbus"))
    val eventBus = new AsyncEventBus(executor, exceptionHandler)
    eventBus.register(deadEventHandler)

    eventBus
  }

  override def singletonStartup(injector: Injector): Unit = {
    val eventBus = injector.instance[EventBus]
    eventBus.register(injector.instance[SchedulerListener])
    eventBus.register(injector.instance[JobListener])
  }
}
