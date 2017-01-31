---
layout: docs
nav: docs
title: "Command: ops"
---

The `ops` command to display information about a particular operator, or list all operators available on a cluster.

## Usage
```
accio ops <options> [<operator name>]
```

If no argument is specified, a summary of available operators will be provided.
If an operator name is specified, information about it will be provided.

## Options
* `-addr=<string>`: Address of the Accio cluster. It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html).
Overrides the ACCIO_ADDR environment variable. Defaults to *127.0.0.1:9999*.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens if specified operator does not exist.
