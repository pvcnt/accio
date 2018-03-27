---
layout: docs
weight: 70
title: "Command: kill"
---

The `kill` command is used to stop a run being executed.

## Usage
```
accio kill [<options>] <run identifier>
```

This command requires a single argument, which is the identifier of the run to kill.
If a parent run is specified, all its children will be stopped.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens when the run does not exist.
* `5`: Internal error.
