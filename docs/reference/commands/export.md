---
layout: docs
weight: 58
title: "Command: export"
---

The `export` command is used to fetch some outputs and write them into organized collections of CSV files.

## Usage
```
accio export [<options>] <run identifier> [<run identifier>...]
```

This command requires as argument one or many run identifiers.

The export generates one CSV file per exported artifact or metric, named after this object (slashes being replaced by dashes).
The first line of each CSV is a header line describing the content;
it is particularly useful when complex data types are exported, to understand the meaning of each column.
The default behavior is to write a file per artifact or metric, with all values found in the runs under consideration.
Activating `-split` or `-aggregate` options allow to alter this behavior.

All artifacts can be exported in CSV, although it is more or less useful depending on their type.
For example, exporting a dataset will only print its URI.
Lists, sets and maps can also be exported, in which case multiple lines will be generated, one per item they contain.

## Options
* `-out=<string>`: Path to a directory where to write the export.
If it does not already exist, it will be created.
If you specify a directory with a previous export, the `-append` will control whether to overwrite or append data.
Defaults to a new random directory created in your working directory.
* `-separator=<string>`: Separator to use in generated files between fields.
Defaults to a space.
* `-artifacts=<string>[,...]`: Include only specified artifacts in the export.
Artifact names must be fully qualified, including node name, e.g., `AreaCoverage/fscore`.
Special artifact names are: *NUMERIC*, to include only artifacts with a numeric type, *ALL* and *NONE*.
Defaults to *NUMERIC*.
* `-metrics=<string>[,...]`: Include only specified metrics in the export.
Metric names must be fully qualified, including node name, e.g., `Promesse/wall_time_millis`.
Special metric names are: *ALL* and *NONE*.
Defaults to *NONE*.
* `-[no]split`: Split the export by workflow parameters.
If specified, you will end up with one directory per combination of workflow parameters across all runs.
Otherwise, artifacts of all runs will be written sequentially in a single file per artifact name.
Defaults to false.
* `-[no]aggregate`: Aggregate artifact values across multiple runs into a single value (the average for numeric types, the concatenation for collection types).
It is only valid for numeric types, pseudo-numeric types (i.e., distance and duration) and collection of those types.
The aggregation operation performed when multiple values are found is the average.
Defaults to false.
* `-[no]append`: Append data at the end of existing files, if they already exist, instead of overwriting them.
The default behavior is to create new files, an incrementing numeric prefix being appended to the file name in case of a conflict.
When using this option, you should ensure that `-split` and `-aggregate` options are the same than for the previous export(s), otherwise you may end up with inconsistent data, as previously exported data will not be altered in any way.
Defaults to false.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
* `5`: Internal error.
