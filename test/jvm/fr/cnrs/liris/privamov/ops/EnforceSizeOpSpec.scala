package fr.cnrs.liris.privamov.ops

import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[EnforceSizeOp]].
 */
class EnforceSizeOpSpec extends UnitSpec with WithTraceGenerator with WithSparkleEnv {
  behavior of "EnforceSizeOp"

  it should "keep traces with a length equal to threshold" in {
    val trace = randomTrace(Me, 15)
    transformMinSize(Seq(trace), 15) should contain theSameElementsAs Seq(trace)
  }

  it should "keep traces with a length greater than threshold" in {
    val trace = randomTrace(Me, 15)
    transformMinSize(Seq(trace), 10) should contain theSameElementsAs Seq(trace)
  }

  it should "reject traces with a length lower than threshold" in {
    val trace = randomTrace(Me, 15)
    transformMinSize(Seq(trace), 16) should have size 0
    transformMinSize(Seq(trace), 20) should have size 0
  }

  private def transformMinSize(data: Seq[Trace], size: Int) = {
    val ds = write(data: _*)
    val res = new EnforceSizeOp(env).execute(EnforceSizeIn(minSize = Some(size), maxSize = None, data = ds), ctx)
    read(res.data)
  }

  private def transformMaxSize(data: Seq[Trace], size: Int) = {
    val ds = write(data: _*)
    val res = new EnforceSizeOp(env).execute(EnforceSizeIn(minSize = Some(size), maxSize = None, data = ds), ctx)
    read(res.data)
  }
}