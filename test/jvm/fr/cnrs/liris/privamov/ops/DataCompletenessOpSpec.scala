package fr.cnrs.liris.privamov.ops

import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[DataCompletenessOp]].
 */
class DataCompletenessOpSpec extends UnitSpec with WithTraceGenerator with WithSparkleEnv {
  behavior of "DataCompleteness"

  it should "compute the data completeness" in {
    val t1 = randomTrace(Me, 120)
    val t2 = randomTrace(Me, 85)
    val metrics = execute(Seq(t1), Seq(t2))
    metrics.value shouldBe Map(Me -> (85d / 120))
  }

  it should "return one for identical traces" in {
    val t1 = randomTrace(Me, 120)
    val metrics = execute(Seq(t1), Seq(t1))
    metrics.value shouldBe Map(Me -> 1d)
  }

  it should "return nothing for an empty trace" in {
    val t1 = randomTrace(Me, 85)
    val t2 = randomTrace(Me, 0)
    val metrics = execute(Seq(t1), Seq(t2))
    metrics.value shouldBe Map(Me -> 0d)
  }

  private def execute(train: Seq[Trace], test: Seq[Trace]) = {
    val trainDs = write(train: _*)
    val testDs = write(test: _*)
    new DataCompletenessOp(env, decoders).execute(DataCompletenessIn(trainDs, testDs), ctx)
  }
}