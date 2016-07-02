package fr.cnrs.liris.accio.testing

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.core.model.{Event, Trace}
import fr.cnrs.liris.common.geo.LatLng
import org.joda.time.Instant

trait WithTraceGenerator {
  protected[this] val Here = LatLng.degrees(48.858222, 2.2945).toPoint
  protected[this] val Now = Instant.now
  protected[this] val Me = "me"
  protected[this] val Him = "him"

  protected def randomTrace(user: String, size: Int, rate: => Duration = Duration.standardSeconds(1)) = {
    if (size <= 0) {
      Trace.empty(user)
    } else {
      Trace(Seq.tabulate(size)(i => Event(user, Here, Now.plus(rate.multipliedBy(i)))))
    }
  }
}