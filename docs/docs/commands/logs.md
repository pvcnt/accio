---
layout: docs
weight: 75
title: "Command: logs"
---

The `logs` command is used to fetch execution logs of a node.

## Usage
```
accio logs [<options>] <run identifier> <node name>
```

This command takes as argument the identifier of a run and the name of a node that is part of this run.
It is possible to distinguish between stdout and stderr with the `-stdout` and `-stderr` flags.
By defaults all logs are returned interleaved, in chronological order, the oldest line being the first result.

## Options
* `-[no]stdout`: Whether to include stdout logs.
Defaults to true.
* `-[no]stderr`: Whether to include stderr logs.
Defaults to true.
* `-n=<integer>`: Limit the number of log lines.
By default all lines are displayed.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens if the run or the node does not exist.
* `5`: Internal error.
