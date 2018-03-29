---
layout: deploy
title: Installing Accio
---

This section is dedicated to system administrators willing to install an Accio cluster.
Documentation about installing a client to connect an existing cluster can be found [in the appropriate section](../docs/install.html).
If you are not familiar with Accio's components, we suggest you read about [its architecture](../docs/architecture.html) first.

Setting up a cluster is essentially about running one or several agents.
We detail next the necessary steps to create your very own cluster.
We only support Linux platforms, and provide more specific instructions for Ubuntu.

* TOC
{:toc}

## Picking up the right components
Because Accio comes with several pluggable parts, the first step is to make choices about which ones are the more appropriate for you.
Some of these choices require that you install required dependencies.

### Scheduler
As of now, only a standalone scheduler is available.
Tasks can be launched on every machine running a copy of the Accio agent in worker mode.
Worker agents are waiting for requests from their master and processing them as they come.
Those agents cannot receive or process requests from clients, and should not be accessible from the outside.

### Storage
Two storages are available: in-memory and Elasticsearch.
Obviously, we recommend using Elasticsearch for production usages, as data will not be persisted otherwise.
In that case, you will first need to [set-up an Elasticsearch cluster](https://www.elastic.co/guide/en/elasticsearch/reference/current/_installation.html).

## Install requirements
Besides dependencies required by pluggable parts you chose previously, the only other requirement is Java 8.
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

## Running the agents
The smallest possible cluster is formed by running one agent in both master and worker mode.
You can also optionally run a gateway, which provides a REST API and Web interface.

First, you need to download the binaries from [the latest release](https://github.com/privamov/accio/releases/latest).
You will need both the `accio-agent.jar` and `accio-executor.jar` files.
These file can be placed anywhere that is suitable for your distribution;
in the remaining of this guide we consider they are simply accessible under `/opt`.
Then, a working directory has to be created to store temporary files generated while running Accio.
It can be anywhere, we assume in the remaining that it is created under `/var/lib/accio-agent`.

Finally, the agent can be launched:

```
java -jar /opt/accio-agent.jar
  -executor_uri=/opt/accio-executor.jar
  -datadir=/var/lib/accio-agent
  -master
  -worker
```

For more information, take a look [the configuration reference](../configuration), describing all configuration parameters and options.

## Supervising the agents
We recommend to run each agent under supervision, whether it is via [Supervisor](http://supervisord.org/), systemd or [Monit](https://mmonit.com/monit/).
We provide examples of scripts for some of these systems in the [`etc/`](https://github.com/privamov/accio/tree/master/etc) source directory.
