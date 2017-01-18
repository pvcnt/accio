---
layout: accio
nav: docs
section: client
title: "Command: run"
---

The `run` command is used to launch a workflow on an Accio cluster.

## Usage
```
accio run [options] <run file>|<package spec>
```

This command requires a single argument, which is either the local path to a file containing a valid run definition, or the specification of a package. A package is specified under the form `<workflow id>[:<workflow version>]`. If the version is not specified, the workflow will be launched at its latest version.

Options of this command allow to supplement or override any field of the run definition. Options have precedence over what is defined by the file or the package specified as argument.

Once a workflow has been successfully launched, this command prints the identifier of the runs that have been created.
The progress of the execution can then be tracked with the `accio status` command and the run identifiers that have been provided.

## General options
* `-addr=<string>`: The address of the Accio cluster. It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html). Overrides the ACCIO_ADDR environment variable. Defaults to *127.0.0.1:9999*.
* `-name=<string>`: Run name. Overrides the value defined in the run file, if any.
* `-tags=<string>[,...]`: Run tags (comma-separated). Overrides the value defined in the run file, if any.
* `-notes=<string>`: Run notes. Overrides the value defined in the run file, if specified.
* `-repeat=<integer>`: Number of times to repeat each run. Overrides the value defined in the run file, if any.
* `-seed=<long>`: Run seed. Overrides the value defined in the run file, if any.
* `-params=<key>=<value>[,...]`: Run parameters (comma-separated). Overrides the values defined in the run file, if any.

## Run options
* `-q`: Prints only run identifiers (one per line), if the command was successful. Otherwise, you can still use the exit code to determine the outcome of the command.
* `-json`: Prints pretty human-readable (but still machine-parsable) JSON.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
* `2`: Invalid run definition.
* `5`: Internal error.
