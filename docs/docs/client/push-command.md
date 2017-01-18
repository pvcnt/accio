---
layout: docs
nav: docs
section: client
title: "Command: push"
---

The `push` command is used to create or update a workflow definition on an Accio cluster.

## Usage
```
accio push [options] <workflow file> [...]
```

This command requires one or many arguments, each one being a local path to a file containing a valid workflow definition.

Once a workflow has been successfully pushed, this command prints the identifier of the workflow that has been created or updated.

## Options
* `-q`: Print only the workflow identifier, if the command was successful. Otherwise, you can still use the exit code to determine the outcome of the command.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
* `2`: Invalid workflow definition.
* `5`: Internal error.
