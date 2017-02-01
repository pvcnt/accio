---
layout: docs
nav: docs
section: client
title: "Command: inspect"
---

The `inspect` command is used to query the status of a specific run.

## Usage
```
accio inspect [<options>] <run id> [<node name>]
```

This command requires a single argument, which is the identifier of the run to inspect.
You can also specify as a second argument a node name that is part of the run.

When used to inspect a run, this command returns information about the run and status of the nodes.
When used to inspect a node, this command returns information about the node as well as, if the node is completed, its outcome: metrics, error (if failed) or artifacts (if successful).
Please note that only a preview of the artifacts is shown, to keep the output readable.
If you want the full results, please enable the `-json` option, or use the [`export` command](export.md).

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
Get status of run 026f42d37ed14401bee76d67dc53ef1d:

```
$ accio inspect 026f42d37ed14401bee76d67dc53ef1d
Id              026f42d37ed14401bee76d67dc53ef1d
Workflow        workflow_Geo-I:657cfdbee26bc779df0a5d0a8070e7970fa505bf
Created         27 minutes ago
Owner           vprimault
Name            <no name>
Tags            <none>
Seed            630005313758984082
Status          Success
Started         27 minutes ago
Completed       26 minutes ago
Duration        76 seconds

== Parameters ==
level          15
pois_threshold 100.0.meters
pois_diameter  200.0.meters
pois_duration  PT900S
url            /data/accio/cabspotting-tree
split_duration PT14400S

== Nodes ==
Node name                       Status     Duration
EventSource                     Success    <cache hit>
DurationSplitting               Success    <cache hit>
TrainPoisExtraction             Success    <cache hit>
GeoIndistinguishability         Success    18 seconds
TestPoisExtraction              Success    17 seconds
AreaCoverage                    Success    24 seconds
SpatialDistortion               Success    55 seconds
PoisRetrieval                   Success    7569 milliseconds
```

Get status of the `PoisRetrieval` node of this run:

```
$ accio inspect e38283871cce42759279d459c8ed45ca PoisRetrieval
Node name       PoisRetrieval
Status          Success
Started         26 minutes ago
Completed       26 minutes ago
Duration        7569 milliseconds
Exit code       0

== Artifacts ==
Name                       Value (preview)
fscore                     orsyalf-54=0.0, odlorhem-5=0.0, <28964 more>
recall                     orsyalf-54=0.0, odlorhem-5=0.0, <28964 more>
precision                  orsyalf-54=1.0, odlorhem-5=1.0, <28964 more>

== Metrics ==
Name                       Value
memory_reserved_bytes      9.32798464E8
wall_time_millis           4938.0
memory_used_bytes          4.83494448E8
cpu_time_millis            2384.0
user_time_millis           2330.0
```