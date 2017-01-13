package org.elasticsearch.test

import java.nio.file.Files

import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.testing.UnitSpec
import org.apache.lucene.util.IOUtils
import org.elasticsearch.client.Client
import org.elasticsearch.cluster.ClusterName
import org.elasticsearch.cluster.metadata.IndexMetaData
import org.elasticsearch.common.network.NetworkModule
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.util.concurrent.EsExecutors
import org.elasticsearch.env.{Environment, NodeEnvironment}
import org.elasticsearch.node.{MockNode, Node}
import org.elasticsearch.plugins.Plugin
import org.elasticsearch.script.ScriptService
import org.elasticsearch.test.discovery.TestZenDiscovery
import org.elasticsearch.transport.MockTcpTransportPlugin
import org.scalatest.BeforeAndAfterEach

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Random

abstract class ElasticUnitSpec extends UnitSpec with BeforeAndAfterEach with StrictLogging {
  private[this] var node: Node = null

  /** Additional settings to add when creating the node. Also allows overriding the default settings. */
  protected def nodeSettings: Settings = Settings.EMPTY

  /** The plugin classes that should be added to the node. */
  protected val plugins = Set.empty[Class[_ <: Plugin]]

  /**
   * This method returns <code>true</code> if the node that is used in the background should be reset
   * after each test. This is useful if the test changes the cluster state metadata etc. The default is
   * <code>false</code>.
   */
  protected def resetNodeAfterTest: Boolean = false

  protected def startNode(): Unit = {
    node = newNode
    println("started node")
    // we must wait for the node to actually be up and running. otherwise the node might have started,
    // elected itself master but might not yet have removed the
    // SERVICE_UNAVAILABLE/1/state not recovered / initialized block
    val clusterHealthResponse = client.admin.cluster.prepareHealth().setWaitForGreenStatus().get
    clusterHealthResponse.isTimedOut shouldBe false

    println("health ok")

    client.admin().indices()
      .preparePutTemplate("one_shard_index_template")
      .setTemplate("*")
      .setOrder(0)
      .setSettings(Settings.builder().put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
        .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0))
      .get()
    println("Started ES node")
  }

  private def stopNode(): Unit = {
    IOUtils.close(node)
    node = null
    logger.info("Stopped ES node")
  }

  protected def client: Client = node.client

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    // Create the node lazily, on the first test. This is ok because we do not randomize any settings,
    // only the cluster name. This allows us to have overridden properties for plugins and the version to use.
    if (node == null) {
      startNode()
    }
  }

  override protected def afterEach(): Unit = {
    logger.info(s"[${getClass.getSimpleName}]: cleaning up after test")
    super.afterEach()
    //assertAcked(client().admin().indices().prepareDelete("*").get());
    val metaData = client.admin().cluster().prepareState().get().getState.getMetaData
    metaData.persistentSettings().getAsMap.size shouldBe 0
    metaData.transientSettings().getAsMap.size shouldBe 0
    if (resetNodeAfterTest) {
      stopNode()
    }
  }

  private def newNode: Node = {
    val tempDir = Files.createTempDirectory("es-test-")
    val settings = Settings.builder()
      .put(ClusterName.CLUSTER_NAME_SETTING.getKey, InternalTestCluster.clusterName("single-node-cluster", Random.nextLong()))
      .put(Environment.PATH_HOME_SETTING.getKey, tempDir)
      .put(Environment.PATH_REPO_SETTING.getKey, tempDir.resolve("repo"))
      .put(Environment.PATH_SHARED_DATA_SETTING.getKey, Files.createTempDirectory("es-test-").getParent)
      .put("node.name", "node_s_0")
      .put("script.stored", "true")
      .put(ScriptService.SCRIPT_MAX_COMPILATIONS_PER_MINUTE.getKey, 1000)
      .put(EsExecutors.PROCESSORS_SETTING.getKey, 1) // limit the number of threads created
      .put(NetworkModule.HTTP_ENABLED.getKey, false)
      .put("transport.type", MockTcpTransportPlugin.MOCK_TCP_TRANSPORT_NAME)
      .put(Node.NODE_DATA_SETTING.getKey, true)
      .put(NodeEnvironment.NODE_ID_SEED_SETTING.getKey, Random.nextLong())
      .put(nodeSettings)
      .build
    var plugins = mutable.Set.empty[Class[_ <: Plugin]] ++ this.plugins
    if (!plugins.contains(classOf[MockTcpTransportPlugin])) {
      plugins += classOf[MockTcpTransportPlugin]
    }
    if (!plugins.contains(classOf[TestZenDiscovery.TestPlugin])) {
      plugins += classOf[TestZenDiscovery.TestPlugin]
    }
    val build = new MockNode(settings, plugins.asJava)
    build.start()
    build
  }

}