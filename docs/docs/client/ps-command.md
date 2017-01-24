---
layout: docs
nav: docs
section: client
title: "Command: ps"
---

The `ps` command is used to search for runs.

## Usage
```
accio ps [options]
```

This command accepts no argument.
By default it returns only scheduled or running runs (up to 100), but options can be specified to alter this behavior.
Runs are returned in reverse chronological order, the most recently created one being the first result.

## Options
* `-addr=<string>`: Address of the Accio cluster. It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html).
Overrides the ACCIO_ADDR environment variable. Defaults to *127.0.0.1:9999*.
* `-no_active`: Hide active (i.e., scheduled or running) runs.
* `-completed`: Include completed (i.e., successful or failed) runs. Implies `-success` and `-failed`.
* `-success`: Include successful runs.
* `-failed`: Include failed runs.
* `-all`: Include all runs, whatever their state. Implies `-completed`.
* `-owner=<string>`: Include only runs belonging to a given owner.
* `-name=<string>`: Include only runs whose name matches a given string.
* `-n=<integer>`: Limit the number of results.
* `-q`: Print only run identifiers, if the command was successful.
Otherwise, you can still use the exit code to determine the outcome of the command.
* `-json`: Print JSON output.

## Exit codes
* `0`: Success.
* `5`: Internal error.