---
layout: docs
nav: docs
title: "Command: help"
---

The `help` command to display help about a particular command, or list all available commands.

## Usage
```
accio help [<command name>]
```

If no argument is specified, a summary of available commands will be provided.
If a command name is specified, information about it will be provided.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens if specified command does not exist.
