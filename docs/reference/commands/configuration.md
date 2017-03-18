---
layout: docs
weight: 10
title: Cluster configuration
---

For now, you can only use a very restricted set of commands, because we have not yet configured our client how to communicate with the Accio cluster.
Configuration is done inside a `clusters.json` file, that is searched in two locations:

  * `/etc/accio/` for system-wide configuration (can be overriden with the `ACCIO_CONFIG_ROOT` environment variable);
  * `~/.accio/` for user-specific configuration.

You may define multiple clusters, each one having at least a name and an address where to contact it.
Clusters defined in these two files are merged, the latter file having the precedence in case of a cluster defined in both files.
The first cluster ever defined is the default cluster, that is used if none is explicitly given.

An simple configuration file looks like this:

```json
[{
  "name": "default",
  "addr": "192.168.50.4:9999"
}]
```

Make sure the specified address matches the one of your actual cluster (it should be the address of a master server).
