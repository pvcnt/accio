---
layout: docs
nav: docs
section: client
title: "Command: list"
---

The `list` command is used to search for workflows.

## Usage
```
accio list [options]
```

This command accepts no argument.
By default it returns all workflows (up to 100), but options can be specified to alter this behavior.
Workflows are returned in reverse chronological order, the most recently created one being the first result.

## Options
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
$ accio list
Id                              Owner            Created          Name
workflow_Cab_Geo-I              Awesome person   1 hour ago       Geo-I Cab nominal workflow
workflow_Cab_W4M                Awesome person   1 hour ago       W4M Cab nominal workflow
workflow_Cab_Promesse           Awesome person   1 hour ago       Promesse Cab nominal workflow
```