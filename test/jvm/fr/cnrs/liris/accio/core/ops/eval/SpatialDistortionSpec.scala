package fr.cnrs.liris.accio.core.ops.eval

import breeze.linalg.{DenseVector, max, min}
import breeze.stats._
import com.google.common.geometry.S1Angle
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.common.util.Distance
import fr.cnrs.liris.testing.UnitSpec
import org.joda.time.Duration

import scala.util.Random

/**
 * Unit tests for [[SpatialDistortionOp]].
 */
class SpatialDistortionSpec extends UnitSpec with WithTraceGenerator {
  val eps = 1e-9

  "SpatialDistortion" should "return zero for identical traces" in {
    val t1 = randomTrace(Me, 120)
    val metrics = new SpatialDistortionOp().evaluate(t1, t1)
    metrics.find(_.name == "avg").get.value shouldBe 0d
    metrics.find(_.name == "min").get.value shouldBe 0d
    metrics.find(_.name == "max").get.value shouldBe 0d
    metrics.find(_.name == "median").get.value shouldBe 0d
    metrics.find(_.name == "stddev").get.value shouldBe 0d
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
    val metrics = new SpatialDistortionOp().evaluate(t1, t2)
    metrics.find(_.name == "avg").get.value shouldBe 0d
    metrics.find(_.name == "min").get.value shouldBe 0d
    metrics.find(_.name == "max").get.value shouldBe 0d
    metrics.find(_.name == "median").get.value shouldBe 0d
    metrics.find(_.name == "stddev").get.value shouldBe 0d
  }

  it should "compute the spatial distortion" in {
    val t1 = randomTrace(Me, 120)
    val distances = DenseVector(Seq.fill(120)(Random.nextInt(5000).toDouble): _*)
    val t2 = t1.replace { events =>
      events.zipWithIndex.map { case (rec, idx) =>
        rec.copy(point = rec.point.translate(S1Angle.degrees(Random.nextInt(360)), Distance.meters(distances(idx))))
      }
    }
    val metrics = new SpatialDistortionOp(interpolate = true).evaluate(t1, t2)
    metrics.find(_.name == "avg").get.value shouldBe (mean(distances) +- eps)
    metrics.find(_.name == "min").get.value shouldBe (min(distances) +- eps)
    metrics.find(_.name == "max").get.value shouldBe (max(distances) +- eps)
    metrics.find(_.name == "median").get.value shouldBe (median(distances) +- eps)
    metrics.find(_.name == "stddev").get.value shouldBe (stddev(distances) +- eps)
  }
}