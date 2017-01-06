package fr.cnrs.liris.accio.executor

import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.routing.ThriftRouter

object ExecutorServerMain extends ExecutorServer

class ExecutorServer extends ThriftServer {

  //override val modules = Seq(DoEverythingModule)

  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .add[ExecutorThriftController]
  }
}