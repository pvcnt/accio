---
layout: docs
nav: docs
section: client
title: "Command: inspect"
---

The `inspect` command is used to query the status of a specific run.

## Usage
```
accio inspect [options] <run id> [<node name>]
```

This command requires as argument the identifier or a run.
You can also specify as a second argument a node name that is part of the run.

When used to inspect a run, this command returns metadata about the run (e.g., name, creation time) and status of the nodes.
When used to inspect a node, this command returns metadata about the node (e.g., creation time, exit code).
If the node is completed, it also prints the outcome: metrics, error information if it failed or artifacts if it was successful.
Please note that when `-json` is not enabled, only a preview of the artifacts is shown, to keep the output readable.
If you want the full results, please enable the `-json` option, or use the [`export` command](export-command.md).

## Options
* `-json`: Print JSON output. If an entire run is being inspected, resulting JSON does *not* include the result of nodes.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens when the run or node does not exist.
* `5`: Internal error.
