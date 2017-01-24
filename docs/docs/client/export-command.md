---
layout: docs
nav: docs
section: client
title: "Command: export"
---

The `export` command is used to fetch some outputs, in an analyzable form.

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
Special artifact name *NUMERIC* can be used to include only artifacts with a numeric type.
Special artifact name *ALL* can be used to include all artifacts.
Defaults to *NUMERIC*.
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

## Examples
Getting the status of a run:

```bash
$ accio inspect e38283871cce42759279d459c8ed45ca
Id              e38283871cce42759279d459c8ed45ca
Workflow        workflow_Cab_Geo-I:ef2388d2c93e724c36933283b5a3815faf7d33a8
Created         2 minutes ago
Owner           vincent
Name            <no name>
Tags            <none>
Status          Scheduled
Progress        17 %
Started         1 minute ago

Node name                       Status
PoisRetrieval                   Waiting
SpatialDistortion               Waiting
GeoIndistinguishability         Waiting
AreaCoverage                    Waiting
EventSource                     Success
DurationSplitting               Running
```

Getting the status of the `EventSource` node of this run:
```bash
$ accio inspect e38283871cce42759279d459c8ed45ca EventSource
Node name       EventSource
Status          Success
Started         3 minutes ago
Completed       2 minutes ago
Exit code       0

Artifact name              Value preview
data                       Dataset(/tmp/accio-agent/uploads/0d684aee0b4bde2704eb937412be92b456a4dcad/data)

Metric name                Value
memory_used_bytes          1.20014304E8
memory_reserved_bytes      2.07683584E8
system_time_nanos          0.0
user_time_nanos            2627000.0
cpu_time_nanos             3400000.0
```