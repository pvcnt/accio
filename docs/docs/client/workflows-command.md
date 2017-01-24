---
layout: docs
nav: docs
section: client
title: "Command: workflows"
---

The `workflows` command is used to search for workflows.

## Usage
```
accio workflows [options]
```

This command accepts no argument.
By default it returns all workflows (up to 100), but options can be specified to alter this behavior.
Workflows are returned in reverse chronological order, the most recently created one being the first result.

## Options
* `-addr=<string>`: Address of the Accio cluster. It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html).
Overrides the ACCIO_ADDR environment variable. Defaults to *127.0.0.1:9999*.
* `-owner=<string>`: Include only workflows belonging to a given owner.
* `-name=<string>`: Include only workflows whose name matches a given string.
* `-n=<integer>`: Limit the number of results.
* `-q`: Print only workflow identifiers, if the command was successful.
Otherwise, you can still use the exit code to determine the outcome of the command.
* `-json`: Print JSON output.

## Exit codes
* `0`: Success.
* `5`: Internal error.

## Examples
List all workflows:

```bash
$ accio workflows
Id                              Owner            Created          Name
workflow_Cab_Geo-I              Awesome person   1 hour ago       Geo-I Cab nominal workflow
workflow_Cab_W4M                Awesome person   1 hour ago       W4M Cab nominal workflow
workflow_Cab_Promesse           Awesome person   1 hour ago       Promesse Cab nominal workflow
```