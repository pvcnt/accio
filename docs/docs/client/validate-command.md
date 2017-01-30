---
layout: docs
nav: docs
section: client
title: "Command: validate"
---

The `validate` command is used to check the syntax and semantics of run and workflow definition files.

## Usage
```
accio validate [options] <definition file> [...]
```

This command requires as arguments one or several paths to files containing workflow definitions or run definitions specified using the appropriate DSL.
It prints any error it finds.
It checks that the JSON syntax is valid, that the JSON schema is respected and that it is semantically correct.
Once validated, a file is guaranteed to be accepted by [push](push-command.html) and [submit](submit-command.html) commands. 

## Options
* `-addr=<string>`: Address of the Accio cluster. It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html).
Overrides the ACCIO_ADDR environment variable. Defaults to *127.0.0.1:9999*.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
* `3`: Failed to validate at least one definition file.
* `5`: Internal error.

