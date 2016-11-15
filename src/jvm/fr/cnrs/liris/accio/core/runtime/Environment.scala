package fr.cnrs.liris.accio.core.runtime

import java.nio.file.Path

import fr.cnrs.liris.accio.core.framework._

import scala.concurrent.Future

case class Job(runId: String, seed: Long, nodeName: String, op: String, inputs: Map[String, Any], localWorkDir: Path)

case class NodeResult(artifacts: Seq[Artifact], stats: NodeExecStats)

trait Environment {
  def submit(job: Job): Future[NodeResult]

  def teardown(): Unit
}