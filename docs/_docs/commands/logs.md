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
By default the standard output is returned, it is possible to return the standard error logs with the `-stderr` flag.
By defaults all logs are returned interleaved, in chronological order, the oldest line being the first result.

## Options
* `-[no]stderr`: Whether to return standard error logs (instead of standard output).
Defaults to false.
* `-n=<integer>`: Limit the number of log lines (taken from the end of the log).
By default all lines are displayed.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens if the run or the node does not exist.
* `5`: Internal error.
