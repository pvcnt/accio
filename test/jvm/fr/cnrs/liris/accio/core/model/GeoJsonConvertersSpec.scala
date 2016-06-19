package fr.cnrs.liris.accio.core.model

import fr.cnrs.liris.accio.core.model.GeoJsonConverters._
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[GeoJsonConverters]].
 */
class GeoJsonConvertersSpec extends UnitSpec with WithTraceGenerator {
  "Record" should "export to GeoJSON" in {
    val json = Record(Me, Here, Now).toGeoJson
    json.geometry shouldBe Here.toGeoJson
    json.properties("time") shouldBe Now.toString
    json.properties("user") shouldBe "me"
  }

  it should "include additional properties when exported to GeoJSON" in {
    val json = Record(Me, Here, Now, Map("foo" -> 1d, "bar" -> 2d)).toGeoJson
    json.properties("foo") shouldBe 1d
    json.properties("bar") shouldBe 2d
  }
}