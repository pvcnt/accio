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

## Examples
Display help about a particular command:

```bash
$ java -jar dist/accio-client.jar help submit
Usage: accio submit <options> <arguments>

Launch an Accio workflow.

Available options:
  - name (type: option; optional)
    Run name
  - tags (type: option; optional)
    Space-separated run tags
  - notes (type: option; optional)
    Run notes
  - repeat (type: option; optional)
    Number of times to repeat each run
  - params (type: option; optional)
    Parameters
  - seed (type: option; optional)
    Seed to use for unstable operators
  - q (type: boolean; default: false)
```