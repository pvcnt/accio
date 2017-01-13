package fr.cnrs.liris.accio.testing

import fr.cnrs.liris.accio.core.domain._

object Tasks {
  val ScheduledTask = Task(
    id = TaskId("id2"),
    runId = RunId("foobar"),
    nodeName = "foonode",
    payload = OpPayload("fooop", 1234, Map.empty),
    key = "fookey",
    scheduler = "dummy",
    createdAt = System.currentTimeMillis(),
    state = TaskState(TaskStatus.Scheduled))
  val RunningTask = Task(
    id = TaskId("id1"),
    runId = RunId("foobar"),
    nodeName = "foonode",
    payload = OpPayload("fooop", 1234, Map.empty),
    key = "fookey",
    scheduler = "dummy",
    createdAt = System.currentTimeMillis(),
    state = TaskState(TaskStatus.Running))
}
