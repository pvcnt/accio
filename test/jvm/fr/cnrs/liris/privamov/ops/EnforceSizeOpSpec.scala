package fr.cnrs.liris.privamov.ops

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[EnforceSizeOp]].
 */
class EnforceSizeOpSpec extends UnitSpec with WithTraceGenerator with WithSparkleEnv {
  behavior of "EnforceSizeOp"

  it should "keep traces with a size greater than min threshold" in {
    val trace = randomTrace(Me, 15)
    transformMinSize(Seq(trace), 10) should contain theSameElementsAs Seq(trace)
    transformMinSize(Seq(trace), 15) should contain theSameElementsAs Seq(trace)
  }

  it should "reject traces with a size lower than min threshold" in {
    val trace = randomTrace(Me, 15)
    transformMinSize(Seq(trace), 16) should have size 0
    transformMinSize(Seq(trace), 20) should have size 0
  }

  it should "shorten traces with a size greater than max threshold" in {
    val trace = randomTrace(Me, size = 15)
    transformMaxSize(Seq(trace), 10) should contain theSameElementsAs Seq(trace.replace(_.take(10)))
  }

  it should "keep traces with a size lower than max threshold" in {
    val trace = randomTrace(Me, size = 15)
    transformMaxSize(Seq(trace), 15) should contain theSameElementsAs Seq(trace)
    transformMaxSize(Seq(trace), 20) should contain theSameElementsAs Seq(trace)
  }

  private def transformMinSize(data: Seq[Trace], size: Int) = {
    val ds = write(data: _*)
    val op = new EnforceSizeOp(env, decoders, encoders)
    val res = op.execute(EnforceSizeIn(minSize = Some(size), maxSize = None, data = ds), ctx)
    read(res.data)
  }

  private def transformMaxSize(data: Seq[Trace], size: Int) = {
    val ds = write(data: _*)
    val op = new EnforceSizeOp(env, decoders, encoders)
    val res = op.execute(EnforceSizeIn(minSize = None, maxSize = Some(size), data = ds), ctx)
    read(res.data)
  }
}