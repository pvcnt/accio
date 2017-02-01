---
layout: docs
nav: docs
title: "Command: push"
---

The `push` command is used to create or update a workflow definition on an Accio cluster.

## Usage
```
accio push [<options>] <workflow file> [...]
```

This command requires one or many arguments, each one being a local path to a file containing a valid workflow definition.

Once a workflow has been successfully pushed, this command prints the identifier of the workflow that has been created or updated.

## Options
* `-addr=<string>`: Address of the Accio cluster. It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html).
Overrides the ACCIO_ADDR environment variable. Defaults to *127.0.0.1:9999*.
* `-quiet`: Print only workflow identifier, if the command was successful.
You can still use the exit code to determine the outcome of the command.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
* `2`: Invalid workflow definition.
* `5`: Internal error.
