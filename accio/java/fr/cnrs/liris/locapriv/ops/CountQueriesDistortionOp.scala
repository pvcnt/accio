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

package fr.cnrs.liris.locapriv.ops

import java.util.concurrent.atomic.AtomicLong

import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.accio.sdk._
import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.sparkle.DataFrame
import fr.cnrs.liris.util.geo.{BoundingBox, Distance, Point}
import org.apache.commons.math3.random.RandomDataGenerator
import org.joda.time.{Duration, Interval}

@Op(
  category = "metric",
  help = "Evaluate count query distortion between to datasets.",
  unstable = true)
case class CountQueriesDistortionOp(
  @Arg(help = "Number of queries to generate")
  n: Int = 1000,
  @Arg(help = "Minimum size of the generated queries' geographical area")
  minSize: Distance = Distance.meters(500),
  @Arg(help = "Maximum size of the generated queries' geographical area")
  maxSize: Distance = Distance.kilometers(2),
  @Arg(help = "Minimum duration of the generated queries' temporal window")
  minDuration: Duration = Duration.standardHours(2),
  @Arg(help = "Maximum duration of the generated queries' temporal window")
  maxDuration: Duration = Duration.standardHours(4),
  @Arg(help = "Train dataset")
  train: RemoteFile,
  @Arg(help = "Test dataset")
  test: RemoteFile)
  extends ScalaOperator[CountQueriesDistortionOp.Out] with SparkleOperator {

  override def execute(ctx: OpContext): CountQueriesDistortionOp.Out = {
    val trainDs = read[Event](train)
    val testDs = read[Event](test)
    val queries = generateQueries(trainDs, ctx.seed)
    val refCounts = queries.count(trainDs)
    val resCounts = queries.count(testDs)
    val metrics = env.parallelize(Seq.tabulate(n)(i => compute(refCounts(i), resCounts(i))))
    CountQueriesDistortionOp.Out(write(metrics, 0, ctx))
  }

  private def generateQueries(trainDs: DataFrame[Event], seed: Long): CountQuerySeq = {
    val rnd = new RandomDataGenerator()
    rnd.reSeed(seed)

    val halfSizeMeters = rnd.nextLong(minSize.meters.round, maxSize.meters.round) / 2d
    val halfDurationMillis = rnd.nextLong(minDuration.getMillis, maxDuration.getMillis) / 2
    val events = trainDs.takeSample(num = n, seed = seed)
    val queries = events.map { event =>
      val bbox = BoundingBox(
        Point(event.point.x - halfSizeMeters, event.point.y - halfSizeMeters),
        Point(event.point.x + halfSizeMeters, event.point.y + halfSizeMeters))
      val span = new Interval(event.time.minus(halfDurationMillis), event.time.plus(halfDurationMillis))
      CountQuery(bbox, span)
    }
    CountQuerySeq(queries)
  }

  private def compute(refCount: Long, resCount: Long) = {
    val distortion = if (0 == refCount) {
      resCount
    } else {
      math.abs(refCount - resCount).toDouble / refCount
    }
    CountQueriesDistortionOp.Value(refCount, resCount, distortion)
  }
}

object CountQueriesDistortionOp {

  case class Value(trainCount: Long, testCount: Long, distortion: Double)

  case class Out(
    @Arg(help = "Metrics dataset")
    metrics: RemoteFile)

}

/**
 * A count query intents to count how many (unique) users are inside a given geographical
 * area (defined by a bounding box) within a given temporal window (defined by a temporal span).
 *
 * @param box  Bounding box.
 * @param span Temporal span.
 */
private case class CountQuery(box: BoundingBox, span: Interval) {
  /**
   * Checks whether a trace contains at least one event inside the spatio-temporal area
   * defined by this query.
   *
   * @param trace Trace.
   * @return True if the trace crosses the area defined by this query, false otherwise.
   */
  def contains(trace: Iterable[Event]): Boolean = {
    trace.exists(r => box.contains(r.point) && span.contains(r.time))
  }
}

/**
 * A list of count queries that can be evaluated in a optimized manner.
 *
 * @param queries List of count queries.
 */
private case class CountQuerySeq(queries: Seq[CountQuery]) {
  /**
   * Evaluate each query in a single iteration over the dataset.
   *
   * @param dataset Dataset of traces.
   * @return Results, in the same order than the queries.
   */
  def count(dataset: DataFrame[Event]): Seq[Long] = {
    val counters = Seq.fill(queries.size)(new AtomicLong)
    dataset.groupBy(_.user).foreach { case (_, trace) =>
      for (i <- counters.indices) {
        if (queries(i).contains(trace)) {
          counters(i).incrementAndGet()
        }
      }
    }
    counters.map(c => c.get)
  }
}