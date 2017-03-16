package fr.cnrs.liris.accio.client.command

import fr.cnrs.liris.accio.agent.AgentService$FinagleClient
import fr.cnrs.liris.accio.client.runtime.Command
import fr.cnrs.liris.common.flags.FlagsProvider

abstract class AbstractCommand(clientProvider: ClusterClientProvider) extends Command {
  protected final def createClient(flags: FlagsProvider): AgentService$FinagleClient = {
    clientProvider(flags.as[ClusterFlags].cluster)
  }
}
