---
layout: privacy
nav: privacy
title: Operators library
---

* TOC
{:toc}

## Source operators

### EventSource

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.EventSourceOp`

Read a dataset of traces.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `url` | string | Dataset URL |
| `kind` | string; optional; default: csv | Kind of dataset |
| `sample` | float; optional | Sampling ratio |
| `users` | list of strings; optional; default: List() | Users to include |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Source dataset |
{: class="table table-striped"}

## Metric operators

### AreaCoverage

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.AreaCoverageOp`

Compute area coverage difference between two datasets of traces

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `level` | integer | S2 cells levels |
| `train` | dataset | Train dataset |
| `test` | dataset | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `precision` | map of strings => floats | Area coverage precision |
| `recall` | map of strings => floats | Area coverage recall |
| `fscore` | map of strings => floats | Area coverage F-score |
{: class="table table-striped"}

### BasicAnalyzer

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.BasicAnalyzerOp`

Compute basic statistics about traces.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `size` | map of strings => longs | Trace size |
| `length` | map of strings => floats | Trace length |
| `duration` | map of strings => longs | Trace duration |
{: class="table table-striped"}

### CountQueriesDistortion

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.CountQueriesDistortionOp`

Evaluate count query distortion between to datasets.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `n` | integer; optional; default: 1000 | Number of queries to generate |
| `minSize` | distance; optional; default: 500.0.meters | Minimum size of the generated queries' geographical area |
| `maxSize` | distance; optional; default: 2000.0.meters | Maximum size of the generated queries' geographical area |
| `minDuration` | duration; optional; default: PT7200S | Minimum duration of the generated queries' temporal window |
| `maxDuration` | duration; optional; default: PT14400S | Maximum duration of the generated queries' temporal window |
| `train` | dataset | Train dataset |
| `test` | dataset | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `value` | list of floats | Count query distortion |
{: class="table table-striped"}

### DataCompleteness

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.DataCompletenessOp`

Compute data completeness difference between two datasets of traces.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | dataset | Train dataset |
| `test` | dataset | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `value` | map of strings => floats | Data completeness |
{: class="table table-striped"}

### PoisAnalyzer

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.PoisAnalyzerOp`

Compute statistics about points of interest

Compute statistics about the POIs that can be extracted from a trace, using a classical DJ-clustering algorithm.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `diameter` | distance | Clustering maximum diameter |
| `duration` | duration | Clustering minimum duration |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `count` | map of strings => longs | POIs count |
| `size` | map of strings => longs | POIs size |
| `duration` | map of strings => longs | POIs duration |
| `sizeRatio` | map of strings => floats | POIs size ratio |
| `durationRatio` | map of strings => floats | POIs duration ratio |
{: class="table table-striped"}

### PoisReident

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.PoisReidentOp`

Re-identification attack using POIs a the discriminating information.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `diameter` | distance | Clustering maximum diameter |
| `duration` | duration | Clustering minimum duration |
| `testDiameter` | distance; optional | Override the clustering maximum diameter to use with the test dataset only |
| `testDuration` | duration; optional | Override the clustering minimum duration to use with the test dataset only |
| `train` | dataset | Train dataset |
| `test` | dataset | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `distances` | map of strings => map of strings => floatss | Distances between users from test and train datasets |
| `matches` | map of strings => strings | Matches between users from test and train datasets |
| `rate` | float | Correct re-identifications rate |
{: class="table table-striped"}

### PoisRetrieval

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.PoisRetrievalOp`

Compute POIs retrieval difference between two datasets of traces

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `diameter` | distance | Clustering maximum diameter |
| `duration` | duration | Clustering minimum duration |
| `threshold` | distance | Matching threshold |
| `train` | dataset | Train dataset |
| `test` | dataset | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `precision` | map of strings => floats | POIs retrieval precision |
| `recall` | map of strings => floats | POIs retrieval recall |
| `fscore` | map of strings => floats | POIs retrieval F-Score |
{: class="table table-striped"}

### SpatialDistortion

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.SpatialDistortionOp`

Compute spatial distortion between two datasets of traces

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `interpolate` | boolean; optional; default: true | Whether to interpolate between points |
| `train` | dataset | Train dataset |
| `test` | dataset | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `min` | map of strings => floats | Spatial distortion min |
| `max` | map of strings => floats | Spatial distortion max |
| `stddev` | map of strings => floats | Spatial distortion stddev |
| `avg` | map of strings => floats | Spatial distortion avg |
| `median` | map of strings => floats | Spatial distortion median |
{: class="table table-striped"}

### TemporalDistortion

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.TemporalDistortionOp`

Compute temporal distortion difference between two datasets of traces

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | dataset | Train dataset |
| `test` | dataset | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `min` | map of strings => floats | Temporal distortion min |
| `max` | map of strings => floats | Temporal distortion max |
| `stddev` | map of strings => floats | Temporal distortion stddev |
| `avg` | map of strings => floats | Temporal distortion avg |
| `median` | map of strings => floats | Temporal distortion median |
{: class="table table-striped"}

### TransmissionDelay

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.TransmissionDelayOp`

Compute transmission delay between two datasets of traces

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | dataset | Train dataset |
| `test` | dataset | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `value` | map of strings => longs | Transmission delay |
{: class="table table-striped"}

## Lppm operators

### GeoIndistinguishability

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.GeoIndistinguishabilityOp`

Enforce geo-indistinguishability guarantees on traces.

Generate locations satisfying geo-indistinguishability properties. The method used here is the one presented by the authors of the paper and consists in adding noise following a double-exponential distribution.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `epsilon` | float; optional; default: 0.001 | Privacy budget |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### Promesse

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.PromesseOp`

Enforce speed smoothing guarantees on traces.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `epsilon` | distance | Distance to enforce between two consecutive points |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

## Prepare operators

### CollapseTemporalGaps

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.CollapseTemporalGapsOp`

Collapse temporal gaps between days.

Removes empty days by shifting data to fill those empty days.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `startAt` | timestamp | Start date for all traces |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### DurationSplitting

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.DurationSplittingOp`

Split traces, ensuring a maximum duration for each one.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `duration` | duration | Maximum duration of each trace |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### EnforceDuration

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.EnforceDurationOp`

Enforce a given duration on each trace.

Longer traces will be truncated, shorter traces will be discarded.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `minDuration` | duration; optional | Minimum duration of a trace |
| `maxDuration` | duration; optional | Maximum duration of a trace |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### EnforceSize

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.EnforceSizeOp`

Enforce a given size on each trace.

Larger traces will be truncated, smaller traces will be discarded.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `minSize` | integer; optional | Minimum number of events in each trace |
| `maxSize` | integer; optional | Maximum number of events in each trace |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### GaussianKernelSmoothing

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.GaussianKernelSmoothingOp`

Apply gaussian kernel smoothing on traces.

Apply gaussian kernel smoothing on a trace, attenuating the impact of noisy observations.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `omega` | duration | Bandwidth |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### SequentialSplitting

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.SequentialSplittingOp`

Split traces sequentially, according to chronological order.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `percentBegin` | float | Percentage of events at which a trace begins |
| `percentEnd` | float | Percentage of events at which a trace ends |
| `complement` | boolean; optional; default: false | Whether to take the complement trace |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### SizeSplitting

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.SizeSplittingOp`

Split traces, ensuring a maximum size for each one.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `size` | integer | Maximum number of events allowed in each trace |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### SpatialGapSplitting

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.SpatialGapSplittingOp`

Split traces, when there is a too huge distance between consecutive events.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `distance` | distance | Maximum distance between two consecutive events |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### SpatialSampling

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.SpatialSamplingOp`

Enforce a minimum distance between two consecutive events in traces.

If the distance is less than a given threshold, records will be discarded until the next point that fulfills the minimum distance requirement.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `distance` | distance | Minimum distance between two consecutive events |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### TemporalGapSplitting

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.TemporalGapSplittingOp`

Split traces, when there is a too long duration between consecutive events.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `duration` | duration | Maximum duration between two consecutive events |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### TemporalSampling

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.TemporalSamplingOp`

Enforce a minimum duration between two consecutive events in traces.

If the duration is less than a given threshold, events will be discarded until the next point that fulfills the minimum duration requirement.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `duration` | duration | Minimum duration between two consecutive events |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

### UniformSampling

:hammer: Implemented in `fr.cnrs.liris.privamov.ops.UniformSamplingOp`

Uniformly sample events inside traces.

Perform a uniform sampling on traces, keeping each event with a given probability.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `probability` | float | Probability to keep each event |
| `data` | dataset | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

