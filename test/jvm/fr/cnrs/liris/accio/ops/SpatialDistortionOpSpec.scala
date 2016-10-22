package fr.cnrs.liris.accio.ops

import breeze.linalg.{DenseVector, max, min}
import breeze.stats._
import com.google.common.geometry.S1Angle
import fr.cnrs.liris.common.util.Distance
import fr.cnrs.liris.privamov.model.Trace
import fr.cnrs.liris.privamov.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec
import org.joda.time.Duration

import scala.util.Random

/**
 * Unit tests for [[SpatialDistortionOp]].
 */
class SpatialDistortionOpSpec extends UnitSpec with WithTraceGenerator with WithSparkleEnv {
  val eps = 1e-7

  behavior of "SpatialDistortion"

  it should "return zero for identical traces" in {
    val t1 = randomTrace(Me, 120)
    val metrics = execute(Seq(t1), Seq(t1), interpolate = false)
    metrics.avg(Me) shouldBe 0d
    metrics.min(Me) shouldBe 0d
    metrics.max(Me) shouldBe 0d
    metrics.median(Me) shouldBe 0d
    metrics.stddev(Me) shouldBe 0d
  }

  it should "return zero for temporally shifted traces" in {
    val t1 = randomTrace(Me, 120)
    val t2 = t1.replace { events =>
      var prev = events.head.time
      events.zipWithIndex.map { case (rec, idx) =>
        val now = prev.plus(Duration.standardSeconds(Random.nextInt(3600)))
        prev = now
        rec.copy(time = now)
      }
    }
    val metrics = execute(Seq(t1), Seq(t2), interpolate = false)
    metrics.avg(Me) shouldBe 0d
    metrics.min(Me) shouldBe 0d
    metrics.max(Me) shouldBe 0d
    metrics.median(Me) shouldBe 0d
    metrics.stddev(Me) shouldBe 0d
  }

  it should "compute the spatial distortion" in {
    val t1 = randomTrace(Me, 120)
    val distances = DenseVector(Seq.fill(120)(Random.nextInt(5000).toDouble): _*)
    val t2 = t1.replace { events =>
      events.zipWithIndex.map { case (rec, idx) =>
        rec.copy(point = rec.point.translate(S1Angle.degrees(Random.nextInt(360)), Distance.meters(distances(idx))))
      }
    }
    val metrics = execute(Seq(t1), Seq(t2), interpolate = true)
    metrics.avg(Me) shouldBe (mean(distances) +- eps)
    metrics.min(Me) shouldBe (min(distances) +- eps)
    metrics.max(Me) shouldBe (max(distances) +- eps)
    metrics.median(Me) shouldBe (median(distances) +- eps)
    metrics.stddev(Me) shouldBe (stddev(distances) +- eps)
  }

  private def execute(train: Seq[Trace], test: Seq[Trace], interpolate: Boolean) = {
    val trainDs = write(train: _*)
    val testDs = write(test: _*)
    new SpatialDistortionOp(env).execute(SpatialDistortionIn(train = trainDs, test = testDs, interpolate = interpolate), ctx)
  }
}