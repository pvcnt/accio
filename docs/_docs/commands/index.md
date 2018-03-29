---
title: Client commands
category: Reference
order: 40
---

Accio comes with a command-line interface that can be used to perform most of the tasks.
There is a single binary, that can be invoked by calling `accio` in your favorite shell, which comes with several commands, e.g., `get`  or `submit`.
The exhaustive list of commands is in the navigation menu on the left.

The command to invoke is specified first, and is generally followed by options and arguments.
By default, if you specify no command, the [`help` command](help.html) will be invoked, giving you access to a built-in help.
Options begin with a single dash, e.g., `-repeat`; their value can be specified either after an equal sign, e.g., `-repeat=3` or after a space, e.g., `-repeat 3`.
There is an exception for boolean options that do not need a value to be specified; instead they can be negated with `no`, e.g., `-color` or `-nocolor` to enable or disabled colored output.
Arguments are everything else that is not part of an option.
Options and arguments for each command are documented on their dedicated help page.

Accio returns an exit code indicating the outcome of the execution.
It follows Unix convention by returning `0` if everything went well and another value in case of an error.
Exit codes for each command are documented on their dedicated help page and can help to disambiguate and failed command.

## Common options
The following options are common to all commands.
They will not be repeated when listing options of each command in this section.

* `-logging=<string>`: Logging level for the client, one of *trace*, *debug*, *info*, *warn*, *error*, *all* or *off*.
Defaults to *warn*.
* `-[no]color`: Enable or disable colored output.
Defaults to true.
* `-[no]quiet`: Suppress all output.
You can still use the exit code to determine the outcome of the command.
Defaults to false.

The `ACCIO_USER` environment variable can be used to specify the actual user's name, which defaults to the current user's shell login.
