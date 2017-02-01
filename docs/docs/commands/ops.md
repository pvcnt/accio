---
layout: docs
nav: docs
title: "Command: ops"
---

The `ops` command to display information about a particular operator, or list all operators available on a cluster.

## Usage
```
accio ops <options> [<operator name>]
```

If no argument is specified, a summary of available operators will be provided.
If an operator name is specified, information about it will be provided.

## Options
* `-addr=<string>`: Address of the Accio cluster.
It can be any name following [Finagle's naming syntax](https://twitter.github.io/finagle/guide/Names.html).
Overrides the ACCIO_ADDR environment variable.
Defaults to *127.0.0.1:9999*.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens if specified operator does not exist.

## Examples
List all available operators (truncated output):

```bash
$ accio ops
Operators in source category:
  EventSource              Read a dataset of traces.
Operators in prepare category:
  DurationSplitting        Split traces, ensuring a maximum duration for each one.
  EnforceDuration          Enforce a given duration on each trace.
  EnforceSize              Enforce a given size on each trace.
  GaussianKernelSmoothing  Apply gaussian kernel smoothing on traces.
Operators in lppm category:
  GeoIndistinguishability  Enforce geo-indistinguishability guarantees on traces.
  Promesse                 Enforce speed smoothing guarantees on traces.
  Wait4Me                  Time-tolerant k-anonymization
Operators in metric category:
  AreaCoverage             Compute area coverage difference between two datasets of traces
  SpatialDistortion        Compute spatial distortion between two datasets of traces
```

Get information about the `GaussianKernelSmoothing` operator:

```bash
$ accio ops GaussianKernelSmoothing
GaussianKernelSmoothing (prepare)

Apply gaussian kernel smoothing on traces.

Apply gaussian kernel smoothing on a trace, attenuating the impact of noisy 
observations.

Available inputs:
  - omega (duration)
    Bandwidth
  - data (dataset)
    Input dataset
Available outputs:
  - data (dataset)
    Output dataset
```