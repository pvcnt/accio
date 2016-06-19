package fr.cnrs.liris.accio.core.ops.eval

import fr.cnrs.liris.accio.core.param.ParamMap
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[DataCompleteness]].
 */
class DataCompletenessSpec extends UnitSpec with WithTraceGenerator {
  "DataCompleteness" should "compute the data completeness" in {
    val t1 = randomTrace(Me, 120)
    val t2 = randomTrace(Him, 85)
    val metrics = new DataCompleteness().evaluate(ParamMap.empty, t1, t2)
    metrics.find(_.name == "value").get.value shouldBe (85d / 120)
  }

  it should "return one for identical traces" in {
    val t1 = randomTrace(Me, 120)
    val metrics = new DataCompleteness().evaluate(ParamMap.empty, t1, t1)
    metrics.find(_.name == "value").get.value shouldBe 1d
  }

  it should "return nothing for an empty trace" in {
    val t1 = randomTrace(Me, 85)
    val t2 = randomTrace(Me, 0)
    val metrics = new DataCompleteness().evaluate(ParamMap.empty, t1, t2)
    metrics.find(_.name == "value").get.value shouldBe 0d
  }
}