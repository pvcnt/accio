---
layout: docs
weight: 80
title: "Command: push"
---

The `push` command is used to create or update a workflow definition on an Accio cluster.

## Usage
```
accio push [<options>] <file> [<file>...]
```

This command requires one or many arguments, each one being a local path to a file containing a valid workflow definition.

Once a workflow has been successfully pushed, this command prints the identifier of the workflow that has been created or updated.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
* `2`: At least one of the workflow definitions was invalid.
* `5`: Internal error.
