---
layout: docs
nav: docs
section: client
title: "Command: rm"
---

The `rm` command is used to delete specific runs.

## Usage
```
accio rm [options] <run id> [...]
```

This command requires as arguments one or several run identifiers.

## Options
* `-addr=<string>`: Address of the Accio cluster. It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html).
Overrides the ACCIO_ADDR environment variable. Defaults to *127.0.0.1:9999*.
* `-quiet`: Suppress output.
You can still use the exit code to determine the outcome of the command.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens when the run or node does not exist.
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
Seed            3320693330992270466
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
Duration        65 seconds
Exit code       0

Artifacts
Name                       Value preview
data                       Dataset(/tmp/accio-agent/uploads/0d684aee0b4bde2704eb937412be92b456a4dcad/data)

Metrics
Name                       Value
memory_used_bytes          1.20014304E8
memory_reserved_bytes      2.07683584E8
system_time_nanos          0.0
user_time_nanos            2627000.0
cpu_time_nanos             3400000.0
```