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
By default it returns only scheduled or running runs, but options can be specified to alter this behavior.
Runs are returned in reverse chronological order, the most recently created one being the first result.

## Options
* `-q`: Print only the run identifiers, if the command was successful. Otherwise, you can still use the exit code to determine the outcome of the command.
* `-json`: Print JSON output.
* `-no_active`: Hide active (i.e., scheduled or running) runs.
* `-completed`: Include completed (i.e., successful or failed) runs. Implies `-success` and `-failed`.
* `-success`: Include successful runs.
* `-failed`: Include failed runs.
* `-all`: Include all runs, whatever their state. Implies `-completed`.
* `-owner=<string>`: Include only runs belonging to a given owner.
* `-n=<integer>`: Limit the results to a given number of runs. Defaults to `50`.

## Exit codes
* `0`: Success.
* `5`: Internal error.


@Flag(name = "q")
  quiet: Boolean = false,
  @Flag(name = "json", help = "Whether to output JSON")
  json: Boolean = false,
  @Flag(name = "active")
  active: Boolean = true,
  @Flag(name = "completed", expansion = Array("success", "failed"))
  completed: Boolean = false,
  @Flag(name = "success")
  success: Boolean = false,
  @Flag(name = "failed")
  failed: Boolean = false,
  @Flag(name = "all")
  all: Boolean = false,
  @Flag(name = "tags")
  tags: Option[String],
  @Flag(name = "owner")
  owner: Option[String],
  @Flag(name = "name")
  name: Option[String],
  @Flag(name = "n")
  n: Option[Int])
