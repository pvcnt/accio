---
layout: docs
nav: docs
title: Operators library
---

* TOC
{:toc}

## Source operators

### EventSource

Read a dataset of traces.

This operator can manipulate the source dataset, essentially to reduce its size, through some basic preprocessing.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `url` | string; required | Dataset URL |
| `kind` | string; optional; default: csv | Kind of dataset |
| `users` | list(string); optional; default:  | Users to include |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Source dataset |
{: class="table table-striped"}

## Prepare operators

### CollapseTemporalGaps

Collapse temporal gaps between days.

Removes empty days by shifting data to fill those empty days.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `startAt` | timestamp; required | Start date for all traces |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### DurationSplitting

Split traces, ensuring a maximum duration for each one.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `duration` | duration; required | Maximum duration of each trace |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### EnforceDuration

Enforce a given duration on each trace.

Longer traces will be truncated, shorter traces will be discarded.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `minDuration` | duration; optional | Minimum duration of a trace |
| `maxDuration` | duration; optional | Maximum duration of a trace |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### EnforceSize

Enforce a given size on each trace.

Larger traces will be truncated, smaller traces will be discarded.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `minSize` | integer; optional | Minimum number of events in each trace |
| `maxSize` | integer; optional | Maximum number of events in each trace |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### GaussianKernelSmoothing

Apply gaussian kernel smoothing on traces.

Apply gaussian kernel smoothing on a trace, attenuating the impact of noisy observations.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `omega` | duration; required | Bandwidth |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### ModuloSampling

Regularly sample events inside traces using the modulo operator.

It will ensure that the final number of events is exactly (+/- 1) the one required, and that events are regularly sampled (i.e., one out of x).

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `n` | integer; required | Number of events to keep |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### SequentialSplitting

Split traces sequentially, according to chronological order.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `percentBegin` | double; required | Percentage of events at which a trace begins |
| `percentEnd` | double; required | Percentage of events at which a trace ends |
| `complement` | boolean; optional; default: false | Whether to take the complement trace |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### SizeSplitting

Split traces, ensuring a maximum size for each one.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `size` | integer; required | Maximum number of events allowed in each trace |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### SpatialGapSplitting

Split traces, when there is a too huge distance between consecutive events.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `distance` | distance; required | Maximum distance between two consecutive events |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### SpatialSampling

Enforce a minimum distance between two consecutive events in traces.

If the distance is less than a given threshold, records will be discarded until the next point that fulfills the minimum distance requirement.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `distance` | distance; required | Minimum distance between two consecutive events |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### TemporalGapSplitting

Split traces, when there is a too long duration between consecutive events.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `duration` | duration; required | Maximum duration between two consecutive events |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### TemporalSampling

Enforce a minimum duration between two consecutive events in traces.

If the duration is less than a given threshold, events will be discarded until the next point that fulfills the minimum duration requirement.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `duration` | duration; required | Minimum duration between two consecutive events |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### UniformSampling

Uniformly sample events inside traces.

Perform a uniform sampling on traces, keeping each event with a given probability.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `probability` | double; required | Probability to keep each event |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

## Lppm operators

### GeoIndistinguishability

Enforce geo-indistinguishability guarantees on traces.

Generate locations satisfying geo-indistinguishability properties. The method used here is the one presented by the authors of the paper and consists in adding noise following a double-exponential distribution.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `epsilon` | double; optional; default: 0.001 | Privacy budget |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### Promesse

Enforce speed smoothing guarantees on traces.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `epsilon` | distance; required | Distance to enforce between two consecutive points |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### Wait4Me

Time-tolerant k-anonymization

Wrapper around the implementation of the Wait4Me algorithm provided by their authors.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `data` | dataset; required | Input dataset |
| `k` | integer; required | Anonymity level |
| `delta` | distance; required | Uncertainty |
| `radiusMax` | distance; optional | Initial maximum radius used in clustering |
| `trashMax` | double; optional; default: 0.1 | Global maximum trash size, in percentage of the dataset size |
| `chunk` | boolean; optional; default: false | Whether to chunk the input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
| `trashSize` | integer | Trash_size |
| `trashedPoints` | long | Number of trashed points |
| `discernibility` | long | Discernibility metric |
| `totalXyTranslations` | distance | Total XY translations |
| `totalTimeTranslations` | duration | Total time translations |
| `xyTranslationsCount` | integer | XY translation count |
| `timeTranslationsCount` | integer | Time translation count |
| `createdPoints` | integer | Number of created points |
| `deletedPoints` | integer | Number of deleted points |
| `meanSpatialTraceTranslation` | distance | Mean spatial translation (per trace) |
| `meanTemporalTraceTranslation` | duration | Mean temporal translation (per trace) |
| `meanSpatialPointTranslation` | distance | Mean spatial translation (per point) |
| `meanTemporalPointTranslation` | duration | Mean temporal translation (per point) |
{: class="table table-striped"}

## Metric operators

### AreaCoverage

Compute area coverage difference between two datasets of traces

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `level` | integer; required | S2 cells levels |
| `width` | duration; optional | Width of time buckets |
| `train` | dataset; required | Train dataset |
| `test` | dataset; required | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `precision` | map(string, double) | Area coverage precision |
| `recall` | map(string, double) | Area coverage recall |
| `fscore` | map(string, double) | Area coverage F-score |
{: class="table table-striped"}

### BasicAnalyzer

Compute basic statistics about traces.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `size` | map(string, long) | Trace size |
| `length` | map(string, double) | Trace length |
| `duration` | map(string, long) | Trace duration |
{: class="table table-striped"}

### CountQueriesDistortion

Evaluate count query distortion between to datasets.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `n` | integer; optional; default: 1000 | Number of queries to generate |
| `minSize` | distance; optional; default: 500.0.meters | Minimum size of the generated queries' geographical area |
| `maxSize` | distance; optional; default: 2000.0.meters | Maximum size of the generated queries' geographical area |
| `minDuration` | duration; optional; default: 2.hours | Minimum duration of the generated queries' temporal window |
| `maxDuration` | duration; optional; default: 4.hours | Maximum duration of the generated queries' temporal window |
| `train` | dataset; required | Train dataset |
| `test` | dataset; required | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `value` | list(double) | Count query distortion |
{: class="table table-striped"}

### DataCompleteness

Compute data completeness difference between two datasets of traces.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | dataset; required | Train dataset |
| `test` | dataset; required | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `value` | map(string, double) | Data completeness |
{: class="table table-striped"}

### HeatMapDistortion

Computes the HeatMaps' distortions between two datasets

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | dataset; required | Input train dataset |
| `test` | dataset; required | Input test dataset |
| `distanceType` | string; optional; default: topsoe | Type of distance metrics between matrices |
| `cellSize` | distance; required | Cell Size in meters |
| `lower` | location; optional; default: -61.0,-131.0 | Lower point |
| `upper` | location; optional; default: 80.0,171.0 | Upper point |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `distortions` | map(string, double) | Distortions ("-" = missing user in train or test)  |
| `avgDist` | double | Average distortion |
{: class="table table-striped"}

### PoisAnalyzer

Compute statistics about points of interest

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `threshold` | distance; required | Matching threshold |
| `train` | dataset; required | Train POIs dataset |
| `test` | dataset; optional | Test POIs dataset, to be compared with train dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | POIs analysis dataset |
{: class="table table-striped"}

### PoisReident

Re-identification attack using POIs a the discriminating information.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | dataset; required | Train dataset (POIs) |
| `test` | dataset; required | Test dataset (POIs) |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `distances` | map(string, map) | Distances between users from test and train datasets |
| `matches` | map(string, string) | Matches between users from test and train datasets |
| `rate` | double | Correct re-identifications rate |
{: class="table table-striped"}

### PoisRetrieval

Compute POIs retrieval difference between two POIs datasets

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `threshold` | distance; required | Maximum distance between two POIs to consider they match |
| `overlap` | duration; optional | Minimum overlap between two POIs to consider they match |
| `train` | dataset; required | Train dataset (POIs) |
| `test` | dataset; required | Test dataset (POIs) |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `precision` | map(string, double) | POIs retrieval precision |
| `recall` | map(string, double) | POIs retrieval recall |
| `fscore` | map(string, double) | POIs retrieval F-Score |
{: class="table table-striped"}

### SpatialDistortion

Compute spatial distortion between two datasets of traces

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `interpolate` | boolean; optional; default: true | Whether to interpolate between points |
| `train` | dataset; required | Train dataset |
| `test` | dataset; required | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `min` | map(string, double) | Spatial distortion min |
| `max` | map(string, double) | Spatial distortion max |
| `stddev` | map(string, double) | Spatial distortion stddev |
| `avg` | map(string, double) | Spatial distortion avg |
| `median` | map(string, double) | Spatial distortion median |
{: class="table table-striped"}

### SpatioTemporalDistortion

Compute temporal distortion difference between two datasets of traces

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | dataset; required | Train dataset |
| `test` | dataset; required | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `min` | map(string, double) | Temporal distortion min |
| `max` | map(string, double) | Temporal distortion max |
| `stddev` | map(string, double) | Temporal distortion stddev |
| `avg` | map(string, double) | Temporal distortion avg |
| `median` | map(string, double) | Temporal distortion median |
{: class="table table-striped"}

### TransmissionDelay

Compute transmission delay between two datasets of traces

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | dataset; required | Train dataset |
| `test` | dataset; required | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `value` | map(string, long) | Transmission delay |
{: class="table table-striped"}

## Transform operators

### PoisExtraction

Compute POIs retrieval difference between two datasets of traces

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `diameter` | distance; required | Clustering maximum diameter |
| `duration` | duration; required | Clustering minimum duration |
| `minPoints` | integer; optional; default: 0 | Minimum number of times a cluster should appear to consider it |
| `data` | dataset; required | Input traces dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output POIs dataset |
{: class="table table-striped"}

