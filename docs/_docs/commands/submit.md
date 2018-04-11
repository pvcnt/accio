---
layout: docs
weight: 85
title: "Command: submit"
---

The `submit` command is used to launch a workflow on an Accio cluster.

## Usage
```
accio submit [<options>] <job file> [param=value [...]]
```

This command requires a single argument, which is either the local path to a file containing a job definition

Options of this command allow to supplement or override any field of the job definition.
Additional arguments are used to specify workflow parameters, under the form `key=value`.
Options and parameters passed as arguments have precedence over what is defined by the job file.

Once a job has been successfully launched, this command prints the identifier of the job that has been created.
The progress of the execution can then be tracked with the `accio describe` command and the job identifier that has been provided.

## Options
* `-addr=<string>`: Address of the Accio cluster.
It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html).
Overrides the `ACCIO_ADDR` environment variable.
Defaults to *127.0.0.1:9999*.
* `-title=<string>`: Run title.
Overrides the value defined in the job file, if any.
* `-tags=<string>[,...]`: Run tags (comma-separated).
Overrides the value defined in the job file, if any.
* `-notes=<string>`: Run notes.
Overrides the value defined in the job file, if specified.
* `-seed=<long>`: Run seed. Overrides the value defined in the job file, if any.
* `-quiet`: Prints only job identifiers (one per line), if the command was successful.
You can still use the exit code to determine the outcome of the command.
* `-json`: Prints pretty human-readable (but still machine-parsable) JSON.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
* `2`: Invalid job definition.
* `5`: Internal error.
