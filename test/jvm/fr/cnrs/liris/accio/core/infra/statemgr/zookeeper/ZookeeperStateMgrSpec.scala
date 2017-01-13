package fr.cnrs.liris.accio.core.infra.statemgr.zookeeper

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import com.twitter.util.{Await, Duration, FuturePool, JavaTimer}
import fr.cnrs.liris.accio.testing.Tasks
import fr.cnrs.liris.testing.UnitSpec
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryOneTime
import org.apache.curator.test.TestingServer
import org.scalatest.BeforeAndAfterEach

class ZookeeperStateMgrSpec extends UnitSpec with BeforeAndAfterEach {
  private[this] var zkTestServer: TestingServer = null
  private[this] var client: CuratorFramework = null

  override protected def beforeEach(): Unit = {
    zkTestServer = new TestingServer(2181)
    client = CuratorFrameworkFactory.newClient(zkTestServer.getConnectString, new RetryOneTime(2000))
    client.start()
  }

  override protected def afterEach(): Unit = {
    client.close()
    zkTestServer.stop()
  }

  behavior of "ZookeeperStateMgr"

  it should "save and retrieve a task" in {
    val stateMgr = new ZookeeperStateMgr(client, "/accio")
    stateMgr.get(Tasks.RunningTask.id) shouldBe None
    stateMgr.save(Tasks.RunningTask)
    stateMgr.get(Tasks.RunningTask.id) shouldBe Some(Tasks.RunningTask)
  }

  it should "list all tasks" in {
    val stateMgr = new ZookeeperStateMgr(client, "/accio")
    stateMgr.save(Tasks.ScheduledTask)
    stateMgr.save(Tasks.RunningTask)
    stateMgr.tasks shouldBe Set(Tasks.ScheduledTask, Tasks.RunningTask)
  }

  it should "delete a task" in {
    val stateMgr = new ZookeeperStateMgr(client, "/accio")
    stateMgr.save(Tasks.RunningTask)
    stateMgr.remove(Tasks.RunningTask.id)
    stateMgr.get(Tasks.RunningTask.id) shouldBe None
  }

  it should "create locks" in {
    /*val stateMgr = new ZookeeperStateMgr(client, "/accio")
    val lock1 = stateMgr.createLock("my/lock")
    val lock2 = stateMgr.createLock("my/lock")
    val pool = FuturePool(Executors.newCachedThreadPool)
    lock1.lock()

    val locked = new AtomicBoolean(false)
    val f = pool {
      lock2.lock()
      locked.set(true)
    }.raiseWithin(Duration.fromSeconds(2))(new JavaTimer)
    println("awaiting")
    Await.ready(f)
    println("awaited")

    // Other thread could not lock.
    locked.get shouldBe false

    println("will unlock")
    lock1.unlock()
    println("unlock")

    lock2.lock()
    lock2.unlock()*/
  }
}