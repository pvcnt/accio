package fr.cnrs.liris.privamov.model

import fr.cnrs.liris.privamov.model.GeoJsonConverters._
import fr.cnrs.liris.privamov.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[GeoJsonConverters]].
 */
class GeoJsonConvertersSpec extends UnitSpec with WithTraceGenerator {
  "Event" should "export to GeoJSON" in {
    val json = Event(Me, Here, Now).toGeoJson
    json.geometry shouldBe Here.toGeoJson
    json.properties("time") shouldBe Now.toString
    json.properties("user") shouldBe "me"
  }

  it should "include additional properties when exported to GeoJSON" in {
    val json = Event(Me, Here, Now, Map("foo" -> 1d, "bar" -> 2d)).toGeoJson
    json.properties("foo") shouldBe 1d
    json.properties("bar") shouldBe 2d
  }
}