package fr.cnrs.liris.accio.core.infra.storage.elastic

import com.twitter.finatra.json.FinatraObjectMapper
import fr.cnrs.liris.accio.core.domain._
import org.elasticsearch.test.ElasticUnitSpec

import scala.collection.{Map, Set}

class ElasticRunRepositorySpec extends ElasticUnitSpec {
  behavior of "ElasticRunRepository"

  it should "save a run" in {
    val repository = new ElasticRunRepository(FinatraObjectMapper.create(), client, "accio")
    println("created reposutory")
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
    println("saved")

    repository.get(RunId("foobar")) shouldBe Some(run)
  }
}