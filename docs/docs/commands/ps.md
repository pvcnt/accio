---
layout: docs
nav: docs
title: "Command: ps"
---

The `ps` command is used to search for runs.

## Usage
```
accio ps [<options>]
```

This command accepts no argument.
By default it returns only scheduled or running runs (up to 100), but options can be specified to alter this behavior.
Runs are returned in reverse chronological order, the most recently created one being the first result.

## Options
* `-addr=<string>`: Address of the Accio cluster. It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html).
Overrides the ACCIO_ADDR environment variable. Defaults to *127.0.0.1:9999*.
* `-[no]active`: Hide active (i.e., scheduled or running) runs.
Defaults to true.
* `-[no]completed`: Include completed (i.e., successful or failed) runs.
Implies `-success` and `-failed`.
Defaults to false.
* `-[no]success`: Include successful runs.
Defaults to false.
* `-[no]failed`: Include failed runs.
Defaults to false.
* `-[no]all`: Include all runs, whatever their state. Implies `-completed`.
Defaults to false.
* `-owner=<string>`: Include only runs belonging to a given owner.
* `-name=<string>`: Include only runs whose name matches a given string.
* `-n=<integer>`: Limit the number of results.
* `-quiet`: Print only run identifiers, if the command was successful.
Otherwise, you can still use the exit code to determine the outcome of the command.
* `-json`: Print JSON output.

## Exit codes
* `0`: Success.
* `5`: Internal error.

## Examples
List all active runs:

```bash
$ accio ps
Run id                            Workflow id      Created          Run name         Status
b69c1e3358f147deb0da8ec9497340aa  workflow_Promes  21 minutes ago   (15) Promesse e  Running
14fa9822df5b4446873cfbcceb69039f  workflow_Geo-I   26 minutes ago   <no name>        Running
```

List my own failed runs:

```bash
$ accio ps -failed -owner=jdoe
Run id                            Workflow id      Created          Run name         Status
6c4a54d5922545e5990fc1f6bd8194c4  workflow_Geo-I   2 days ago       <no name>        Failed
```