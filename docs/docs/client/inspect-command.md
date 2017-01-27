---
layout: docs
nav: docs
section: client
title: "Command: inspect"
---

The `inspect` command is used to query the status of a specific run.

## Usage
```
accio inspect [options] <run id> [<node name>]
```

This command requires as argument the identifier of a run.
You can also specify as a second argument a node name that is part of the run.

When used to inspect a run, this command returns metadata about the run (e.g., name, creation time) and status of the nodes.
When used to inspect a node, this command returns metadata about the node (e.g., creation time, exit code).
If the node is completed, it also prints the outcome: metrics, error information if it failed or artifacts if it was successful.
Please note that when `-json` is not enabled, only a preview of the artifacts is shown, to keep the output readable.
If you want the full results, please enable the `-json` option, or use the [`export` command](export-command.md).

## Options
* `-addr=<string>`: Address of the Accio cluster. It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html).
Overrides the ACCIO_ADDR environment variable. Defaults to *127.0.0.1:9999*.
* `-json`: Print JSON output. If an entire run is being inspected, resulting JSON does *not* include the result of nodes.

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