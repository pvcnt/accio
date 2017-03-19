---
layout: docs
weight: 90
title: "Command: validate"
---

The `validate` command is used to check the syntax and semantics of run and workflow definition files.

## Usage
```
accio validate [<options>] <file> [<file>...]
```

This command takes as argument paths to files containing workflow or run definitions to validate.
It prints any error it finds.
It checks that the JSON syntax is valid, that the JSON schema is respected and that it is semantically correct.
For example, operator names are verified et valid dependencies between nodes are enforced.
Once validated, a file is guaranteed to be accepted by [push](push.html) and [submit](submit.html) commands.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens if no file were specified.
* `2`: Failed to validate at least one file.
* `5`: Internal error.
