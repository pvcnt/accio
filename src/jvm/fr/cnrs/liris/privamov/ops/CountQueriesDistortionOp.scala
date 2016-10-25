/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.privamov.ops

import java.util.concurrent.atomic.AtomicLong

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.common.geo.{BoundingBox, Point}
import fr.cnrs.liris.common.random.RandomUtils
import fr.cnrs.liris.common.geo.Distance
import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.core.sparkle.{DataFrame, SparkleEnv}
import org.apache.commons.math3.random.RandomDataGenerator
import org.joda.time.{Duration, Interval}

@Op(
  category = "metric",
  help = "Evaluate count query distortion between to datasets.")
class CountQueriesDistortionOp @Inject()(env: SparkleEnv) extends Operator[CountQueriesDistortionIn, CountQueriesDistortionOut] with SparkleOperator {

  override def execute(in: CountQueriesDistortionIn, ctx: OpContext): CountQueriesDistortionOut = {
    val trainDs = read(in.train, env)
    val testDs = read(in.test, env)
    val generator = new CountQueryGenerator(trainDs, ctx.seed, in.minSize, in.maxSize, in.minDuration, in.maxDuration)
    val queries = generator.generate(in.n)
    val refCounts = queries.count(trainDs)
    val resCounts = queries.count(testDs)
    val values = refCounts.indices.map(i => compute(refCounts(i), resCounts(i)))
    CountQueriesDistortionOut(values)
  }

  private def compute(refCount: Long, resCount: Long): Double = {
    if (0 == refCount) resCount
    else Math.abs(refCount - resCount).toDouble / refCount
  }
}

case class CountQueriesDistortionIn(
  @Arg(help = "Number of queries to generate") n: Int = 1000,
  @Arg(help = "Minimum size of the generated queries' geographical area") minSize: Distance = Distance.meters(500),
  @Arg(help = "Maximum size of the generated queries' geographical area") maxSize: Distance = Distance.kilometers(2),
  @Arg(help = "Minimum duration of the generated queries' temporal window") minDuration: Duration = Duration.standardHours(2),
  @Arg(help = "Maximum duration of the generated queries' temporal window") maxDuration: Duration = Duration.standardHours(4),
  @Arg(help = "Train dataset") train: Dataset,
  @Arg(help = "Test dataset") test: Dataset)

case class CountQueriesDistortionOut(
  @Arg(help = "Count query distortion") value: Seq[Double])

/**
 * A count query intents to count how many (unique) users are inside a given geographical
 * area (defined by a bounding box) within a given temporal window (defined by a temporal span).
 *
 * @param box  Bounding box.
 * @param span Temporal span.
 */
private case class CountQuery(box: BoundingBox, span: Interval) {
  /**
   * Execute the query over a dataset and returns the result.
   *
   * @param dataset Dataset of traces.
   * @return Number of traces matching this query.
   */
  def count(dataset: DataFrame[Trace]): Long = dataset.filter(contains).count()

  /**
   * Checks whether a trace contains at least one event inside the spatio-temporal area
   * defined by this query.
   *
   * @param trace Trace.
   * @return True if the trace crosses the area defined by this query, false otherwise.
   */
  def contains(trace: Trace): Boolean =
  trace.events.exists(r => box.contains(r.point) && span.contains(r.time))
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
  def count(dataset: DataFrame[Trace]): Seq[Long] = {
    val counters = queries.indices.map(idx => new AtomicLong)
    dataset.foreach(trace => {
      for (i <- counters.indices) {
        if (queries(i).contains(trace)) {
          counters(i).incrementAndGet()
        }
      }
    })
    counters.map(c => c.get())
  }
}

/**
 * Generates a bunch of random range queries from a dataset and spatio-temporal areas sizes.
 *
 * @param data        Dataset of traces to generate queries from.
 * @param seed        Seed.
 * @param minSize     Minimum size of the geographical area.
 * @param maxSize     Maximum size of the geographical area.
 * @param minDuration Minimum durations of the temporal window.
 * @param maxDuration Minimum duration of the temporal window.
 */
private class CountQueryGenerator(data: DataFrame[Trace], seed: Long, minSize: Distance, maxSize: Distance, minDuration: Duration, maxDuration: Duration) {
  private[this] val rnd = new RandomDataGenerator()
  rnd.reSeed(seed)

  /**
   *
   * @return
   */
  def generate(): CountQuery = generate(1).queries.head

  def generate(nb: Int): CountQuerySeq = {
    val halfSizeMeters = rnd.nextLong(minSize.meters.round, maxSize.meters.round) / 2d
    val halfDurationMillis = rnd.nextLong(minDuration.getMillis, maxDuration.getMillis) / 2
    val traces = data.takeSample(withReplacement = true, num = nb)
    val queries = traces.map { trace =>
      val event = RandomUtils.randomElement(trace.events, seed)
      val box = BoundingBox(Point(event.point.x + halfSizeMeters, event.point.y + halfSizeMeters), Point(event.point.x - halfSizeMeters, event.point.y - halfSizeMeters))
      val span = new Interval(event.time.minus(halfDurationMillis), event.time.plus(halfDurationMillis))
      CountQuery(box, span)
    }
    CountQuerySeq(queries)
  }
}