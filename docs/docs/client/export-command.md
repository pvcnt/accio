---
layout: docs
nav: docs
section: client
title: "Command: export"
---

The `export` command is used to fetch some outputs and write them into organized collections of CSV files.

## Usage
```
accio export [options] <run id> [...]
```

This command requires as argument one or many run identifiers.

## Options
* `-addr=<string>`: Address of the Accio cluster. It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html).
Overrides the ACCIO_ADDR environment variable. Defaults to *127.0.0.1:9999*.
* `-out=<string>`: Path to a directory where to write the export.
If it does not already exist, it will be created.
If you specify a directory with a previous export, the `-append` will control whether to overwrite or append data.
Defaults to a new random directory created in your working directory.
* `-separator=<string>`: Separator to use in generated files between fields. Defaults to a space.
* `-artifacts=<string>[,...]`: Include only specified artifacts in the export.
Special artifact names are: *NUMERIC*, to include only artifacts with a numeric type, *ALL* and *NONE*.
Defaults to *NUMERIC*.
* `-metrics=<string>[,...]`: Include only specified metrics in the export.
Special metric names are: *ALL* and *NONE*.
Defaults to *NONE*.
* `-split`: Split the export by workflow parameters.
If specified, you will end up with one directory per combination of workflow parameters across all runs.
Otherwise, artifacts of all runs will be written sequentially in a single file per artifact name.
* `-aggregate`: Aggregate artifact values across multiple runs into a single value (the average for numeric types, the concatenation for collection types).
It is only valid for numeric types and collection of numeric types.
* `-append`: Append data at the end of existing files, if they already exist, instead of overwriting them.
The default behavior is to replace content of existing files, but you can choose to append new data at the end.
When using this option, you should ensure that `-split` and `-aggregate` options are the same than for the previous export(s), otherwise you may end up with inconsistent data, as previously exported data will not be altered in any way.


## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
* `5`: Internal error.