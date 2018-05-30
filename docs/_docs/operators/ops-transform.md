---
title: "Category: Transform"
order: 53
---

* TOC
{:toc}

## CollapseTemporalGaps

Removes empty days by shifting data to fill those empty days.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `startAt` | an instant; required | Start date for all traces |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## DurationSplitting

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `duration` | a duration; required | Maximum duration of each trace |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## EnforceDuration

Longer traces will be truncated, shorter traces will be discarded.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `minDuration` | a duration; optional | Minimum duration of a trace |
| `maxDuration` | a duration; optional | Maximum duration of a trace |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## EnforceSize

Larger traces will be truncated, smaller traces will be discarded.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `minSize` | a 32-bit integer; optional | Minimum number of events in each trace |
| `maxSize` | a 32-bit integer; optional | Maximum number of events in each trace |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## GaussianKernelSmoothing

Apply gaussian kernel smoothing on a trace, attenuating the impact of noisy observations.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `omega` | a duration; required | Bandwidth |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## ModuloSampling

It will ensure that the final number of events is exactly (+/- 1) the one required, and that events are regularly sampled (i.e., one out of x).

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `n` | a 32-bit integer; required | Number of events to keep |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## PoisExtraction

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `diameter` | a distance; required | Clustering maximum diameter |
| `duration` | a duration; required | Clustering minimum duration |
| `minPoints` | a 32-bit integer; optional; default: 0 | Minimum number of times a cluster should appear to consider it |
| `data` | a file; required | Input traces dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## SequentialSplitting

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `percentBegin` | a 32-bit integer; required | Percentage of events at which a trace begins |
| `percentEnd` | a 32-bit integer; required | Percentage of events at which a trace ends |
| `complement` | a boolean; optional; default: false | Whether to take the complement trace |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## SizeSplitting

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `size` | a 32-bit integer; required | Maximum number of events allowed in each trace |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## SpatialGapSplitting

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `distance` | a distance; required | Maximum distance between two consecutive events |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## SpatialSampling

If the distance is less than a given threshold, records will be discarded until the next point that fulfills the minimum distance requirement.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `distance` | a distance; required | Minimum distance between two consecutive events |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## TemporalGapSplitting

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `duration` | a duration; required | Maximum duration between two consecutive events |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## TemporalSampling

If the duration is less than a given threshold, events will be discarded until the next point that fulfills the minimum duration requirement.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `duration` | a duration; required | Minimum duration between two consecutive events |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## UniformSampling

Perform a uniform sampling on traces, keeping each event with a given probability.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `probability` | a 64-bit float; required | Probability to keep each event |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

