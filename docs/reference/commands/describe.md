---
layout: docs
weight: 55
title: "Command: describe"
---

The `inspect` command is used to describe a specific resource.

## Usage
```
accio inspect [<options>] <run id> [<node name>]
```

This command requires a single argument, which is the identifier of the run to inspect.
You can also specify as a second argument a node name that is part of the run.

When used to inspect a run, this command returns information about the run and status of the nodes.
When used to inspect a node, this command returns information about the node as well as, if the node is completed, its outcome: metrics, error (if failed) or artifacts (if successful).
Please note that only a preview of the artifacts is shown, to keep the output readable.
If you want the full results, please enable the `-json` option, or use the [`export` command](export.md).

## Options
* `-addr=<string>`: Address of the Accio cluster. It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html).
Overrides the ACCIO_ADDR environment variable. Defaults to *127.0.0.1:9999*.
* `-json`: Print JSON output. If an entire run is being inspected, resulting JSON does *not* include the result of nodes.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens when the run or node does not exist.
* `5`: Internal error.

## Examples
```
# Get status of run 026f42d37ed14401bee76d67dc53ef1d:
$ accio inspect 026f42d37ed14401bee76d67dc53ef1d

# Get status of the `PoisRetrieval` node of this run:
$ accio inspect e38283871cce42759279d459c8ed45ca PoisRetrieval
```
