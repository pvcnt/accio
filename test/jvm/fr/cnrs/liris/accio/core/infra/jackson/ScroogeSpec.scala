package fr.cnrs.liris.accio.core.infra.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import fr.cnrs.liris.testing.UnitSpec

abstract class ScroogeSpec extends UnitSpec {
  protected val mapper: ObjectMapper = {
    val mapper = new ObjectMapper
    mapper.registerModule(ScroogeModule)
    mapper
  }
}