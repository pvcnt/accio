---
layout: docs
nav: docs
section: client
title: "Command: help"
---

The `help` command to display help on a particular topic, or help summary.

## Usage
```
accio help <help topic>
```

If no argument is specified, a summary of available commands and help topics will be provided.
If a help topic is specified, information about it will be provided.
A valid help topic is either a command name or an operator name.
It can also be a special key:

  * `list-ops`: Provide the list of registered operators.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens if specified help topic does not exist.
