package fr.cnrs.liris.privamov.testing

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.common.geo.LatLng
import fr.cnrs.liris.privamov.core.model.{Event, Trace}
import org.joda.time.Instant

import scala.collection.mutable

trait WithTraceGenerator {
  protected[this] val Here = LatLng.degrees(48.858222, 2.2945).toPoint
  protected[this] val Now = Instant.now
  protected[this] val Me = "me"
  protected[this] val Him = "him"

  protected def randomTrace(user: String, size: Int, rate: => Duration = Duration.standardSeconds(1)) = {
    if (size <= 0) {
      Trace.empty(user)
    } else {
      val events = mutable.ListBuffer.empty[Event]
      var now = Now
      for (i <- 0 until size) {
        events += Event(user, Here, now)
        now = now + rate
      }
      Trace(events.toList)
    }
  }
}