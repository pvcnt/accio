---
layout: docs
nav: docs
section: client
title: Accio client (CLI)
---

## Common options
The following options are common to all commands.
They will not be repeated when listing the options of each command in this section.

* `-logging=<string>`: Logging level for the client, one of *trace*, *debug*, *info*, *warn*, *error*, *all* or *off*. Defaults to *warn*.
* `-rc=<string>`: Path to the .acciorc configuration file.
* `-config=<string>`: Name of the configuration to use, conjointly with the .acciorc configuration file.
* `-no_color`: Disable colored output.

The `ACCIO_USER` environment variable can be used to specify the actual user's name.