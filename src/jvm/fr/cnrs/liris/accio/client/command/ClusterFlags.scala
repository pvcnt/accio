package fr.cnrs.liris.accio.client.command

import fr.cnrs.liris.common.flags.Flag

case class ClusterFlags(@Flag(name = "cluster") cluster: Option[String])
