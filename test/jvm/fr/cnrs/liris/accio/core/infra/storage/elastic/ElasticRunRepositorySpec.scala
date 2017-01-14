package fr.cnrs.liris.accio.core.infra.storage.elastic

import com.google.inject.Guice
import com.sksamuel.elastic4s.testkit.ElasticSugar
import com.twitter.finatra.json.FinatraObjectMapper
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.infra.jackson.AccioFinatraJacksonModule
import fr.cnrs.liris.testing.UnitSpec

import scala.collection.{Map, Set}

class ElasticRunRepositorySpec extends UnitSpec with ElasticSugar {
  private val repository = {
    val injector = Guice.createInjector(AccioFinatraJacksonModule)
    new ElasticRunRepository(injector.getInstance(classOf[FinatraObjectMapper]), client, "accio")
  }

  behavior of "ElasticRunRepository"

  it should "save a run" in {
    val run = Run(
      id = RunId("foobar"),
      pkg = Package(WorkflowId("my_workflow"), "v1"),
      cluster = "local",
      environment = "devel",
      owner = User("m"),
      name = Some("foo bar workflow"),
      notes = Some("awesome workflow!"),
      tags = Set("foo", "bar"),
      seed = 1234,
      params = Map.empty,
      createdAt = System.currentTimeMillis(),
      state = RunState(status = RunStatus.Scheduled, progress = 0))
    repository.save(run)

    repository.get(RunId("foobar")) shouldBe Some(run)
  }
}