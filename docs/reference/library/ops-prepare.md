---
layout: docs
title: "Operators: Prepare"
weight: 52
---

* TOC
{:toc}

## CollapseTemporalGaps

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

## DurationSplitting

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `duration` | duration; required | Maximum duration of each trace |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

## EnforceDuration

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

## EnforceSize

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

## GaussianKernelSmoothing

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

## ModuloSampling

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

## SequentialSplitting

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

## SizeSplitting

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `size` | integer; required | Maximum number of events allowed in each trace |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

## SpatialGapSplitting

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `distance` | distance; required | Maximum distance between two consecutive events |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

## SpatialSampling

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

## TemporalGapSplitting

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `duration` | duration; required | Maximum duration between two consecutive events |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

## TemporalSampling

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

## UniformSampling

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

