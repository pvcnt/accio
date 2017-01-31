---
layout: docs
nav: docs
title: "Command: version"
---

The `version` command is used to get client and agent build information.

## Usage
```
accio version [<options>]
```

This command requires no argument.
It prints the current version of the Accio client binary, and the version of the agent running on the Accio cluster.

## Options
* `-addr=<string>`: Address of the Accio cluster.
It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html).
Overrides the ACCIO_ADDR environment variable. Defaults to *127.0.0.1:9999*.

## Exit codes
* `0`: Success.