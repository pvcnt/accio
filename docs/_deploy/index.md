---
layout: deploy
title: Installing Accio
---

This section is dedicated to system administrators willing to deploy an Accio cluster.
Documentation about installing a client to connect an existing cluster can be found [in the appropriate section](../docs/install.html).
If you are not familiar with Accio's components, we suggest you read about [its architecture](../docs/architecture.html) first.

Setting up a cluster is essentially about running one or several agents.
We only support Linux platforms, and provide more specific instructions for Ubuntu.

* TOC
{:toc}

## Install requirements
The only strict requirement is to have Java 8.
More specifically, you need a JRE (Java Runtime Environment) to execute Accio.

**Ubuntu Trusty (14.04 LTS).**
OpenJDK 8 is not available on Trusty (if you install the JRE through your package manager, it will install Java 7).
To install Oracle JRE 8, you need to first add a PPA repository.
```bash
sudo add-apt-repository ppa:openjdk-r/ppa
sudo apt-get update
sudo apt-get install openjdk-8-jre
```

Note: You might need to install the `software-properties-common` package if you don't have the `add-apt-repository` command.
```bash
sudo apt-get install software-properties-common
```

**Ubuntu Wily (15.10) and later.**
OpenJDK 8 is packaged with recent versions of Ubuntu, and can be installed straight from the package manager.
```bash
sudo apt-get install openjdk-8-jre
```

## Download the binaries
First, you need to download the binaries from [the latest release](https://github.com/privamov/accio/releases/latest).
You will need both the `accio-agent.jar` and `accio-executor.jar` files.
If you want to run a gateway (which is recommended in most settings), you should also download `accio-gateway.jar`.
These file can be placed anywhere that is suitable for your distribution; in the remaining of this guide we consider they are placed under `/opt`.

```bash
ACCIO_VERSION=0.7.0
curl -L -O "https://github.com/privamov/accio/releases/download/v$ACCIO_VERSION/accio-agent.jar"
curl -L -O "https://github.com/privamov/accio/releases/download/v$ACCIO_VERSION/accio-executor.jar"
curl -L -O "https://github.com/privamov/accio/releases/download/v$ACCIO_VERSION/accio-gateway.jar"
sudo mv accio-agent.jar /opt
sudo mv accio-executor.jar /opt
sudo mv accio-gateway.jar /opt
```

## Install the agent

The agent needs a data directory for him to persist some files.
It can be anywhere, we assume in the remaining that it is created under `/var/lib/accio-agent`:
```bash
sudo mkdir /var/lib/accio-agent
sudo chown $USER: /var/lib/accio-agent
```

Finally, the agent can be launched with the following command:
```bash
java -jar /opt/accio-agent.jar
  -executor_uri=/opt/accio-executor.jar
  -datadir=/var/lib/accio-agent
```

This command starts the agent by specifying the location of the executor and where it can store data.
Those are the only two options required to start the agent, all of the other options have default values.
The output should be similar to this:
```
...
[main] INFO  f.c.l.accio.agent.AgentServerMain$ - Thrift server started on port 9999
[main] INFO  f.c.l.accio.agent.AgentServerMain$ - Enabling health endpoint on port 9990
[main] INFO  f.c.l.accio.agent.AgentServerMain$ - fr.cnrs.liris.accio.agent.AgentServerMain started.
[main] INFO  f.c.l.accio.agent.AgentServerMain$ - Startup complete, server ready.
```

It indicates that the server is listening on port 9999 (it speaks the Thrift protocol, not HTTP), while an administrative server is listening on port 9990 (this one speaks HTTP).
The administrative server is not intended to be publicly exposed, but should stay behind your firewall.

The previous command launches the agent in the foreground.
We recommend to run each agent under supervision, whether it is via systemd, [Supervisor](http://supervisord.org/) or [Monit](https://mmonit.com/monit/).
Those systems allow to automatically start and restart the agent, ensuring it is always running and healthy.
Examples of such configuration files may be found in the [`etc/` directory](https://github.com/privamov/accio/tree/master/etc).

Now, you should be interested in [configuring the agent](configuration.html), because the default options may not be suited for production use.
For example, an in-memory storage is used by default, which means that all data will be list when the agent stops.
You may also be interested in [adding some security](security.html) to prevent anybody from accessing the agent API.

## Install the client

A client is needed in order to interact with the server.
The installation instructions are available [on this page](../docs/install.html).

## Install the gateway

The gateway provides a REST interface to Accio, in addition to an optional Web interface.
The agent can be launched with the following command:
```bash
java -jar /opt/accio-gateway.jar
  -admin.port=:9991
  -agent.server=localhost:9999
  -ui
```

The address of the agent is specified with the `-agent.server` option, and we ask for the Web interface to be exposed with the `-ui` option.
Finally, because the administrative server would by default be started on the same port than the agent, we rebind it on another port.
Again, the administrative server is not intended to be publicly exposed, but should stay behind your firewall.
