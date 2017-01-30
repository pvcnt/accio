---
layout: docs
nav: docs
section: client
title: "Command: rm"
---

The `kill` command is used to stop a run being executed.

## Usage
```
accio rm [options] <run id>
```

This command requires a single argument, which is the identifier of the run to kill.
If a parent run is specified, all its children will be stopped. 

## Options
* `-addr=<string>`: Address of the Accio cluster. It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html).
Overrides the ACCIO_ADDR environment variable. Defaults to *127.0.0.1:9999*.
* `-quiet`: Suppress output.
You can still use the exit code to determine the outcome of the command.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens when the run does not exist.
* `5`: Internal error.