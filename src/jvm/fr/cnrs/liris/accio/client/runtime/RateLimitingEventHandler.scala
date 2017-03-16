package fr.cnrs.liris.accio.client.runtime

import com.twitter.util.{Duration, Time}
import fr.cnrs.liris.accio.client.event.{Event, EventHandler, EventKind}

/**
 * Creates a new Event handler that rate limits the events of type PROGRESS
 * to one per event "rateLimitation" seconds.  Events that arrive too quickly are dropped;
 * all others are are forwarded to the handler "delegateTo".
 *
 * @param handler   The event handler that ultimately handles the events
 * @param rateLimit The duration between events that will be forwarded to the underlying handler.
 */
class RateLimitingEventHandler(handler: EventHandler, rateLimit: Duration) extends EventHandler {
  require(rateLimit > Duration.Zero, "Rate limit must be strictly positive")
  private[this] var lastEvent = Time.Undefined

  override def handle(event: Event): Unit = {
    event.kind match {
      case EventKind.Progress | EventKind.Start | EventKind.Finish =>
        val currentTime = Time.now
        if (lastEvent == Time.Undefined || lastEvent + rateLimit <= currentTime) {
          lastEvent = currentTime
          handler.handle(event)
        }
      case _ => handler.handle(event)
    }
  }
}