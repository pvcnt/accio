---
title: "Category: Metric"
order: 51
---

* TOC
{:toc}

## AreaCoverage

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `level` | a 32-bit integer; required | S2 cells levels |
| `width` | a duration; optional | Width of time buckets |
| `train` | a file; required | Train dataset |
| `test` | a file; required | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `metrics` | a file | Area coverage metrics |
{: class="table table-striped"}

## CountQueriesDistortion

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `n` | a 32-bit integer; optional; default: 1000 | Number of queries to generate |
| `minSize` | a distance; optional; default: 500.0.meters | Minimum size of the generated queries' geographical area |
| `maxSize` | a distance; optional; default: 2000.0.meters | Maximum size of the generated queries' geographical area |
| `minDuration` | a duration; optional; default: PT7200S | Minimum duration of the generated queries' temporal window |
| `maxDuration` | a duration; optional; default: PT14400S | Maximum duration of the generated queries' temporal window |
| `train` | a file; required | Train dataset |
| `test` | a file; required | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `metrics` | a file | Metrics dataset |
{: class="table table-striped"}

## DataCompleteness

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | a file; required | Train dataset |
| `test` | a file; required | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `metrics` | a file | Data completeness |
{: class="table table-striped"}

## HeatMapDistortion

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | a file; required | Input train dataset |
| `test` | a file; required | Input test dataset |
| `distanceType` | a string; optional; default: topsoe | Type of distance metrics between matrices |
| `cellSize` | a distance; required | Cell Size in meters |
| `lowerLat` | a 64-bit float; optional; default: -61.0 | Lower point latitude |
| `lowerLng` | a 64-bit float; optional; default: -131.0 | Lower point longitude |
| `upperLat` | a 64-bit float; optional; default: 80.0 | Upper point latitude |
| `upperLng` | a 64-bit float; optional; default: 171.0 | Upper point longitude |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `distortions` | a file | Metrics dataset |
| `avgDist` | a 64-bit float | Average distortion |
{: class="table table-striped"}

## PoisReident

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | a file; required | Train dataset (POIs) |
| `test` | a file; required | Test dataset (POIs) |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `metrics` | a file | Metrics dataset |
| `rate` | a 64-bit float | Correct re-identifications rate |
{: class="table table-striped"}

## PoisRetrieval

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `threshold` | a distance; required | Maximum distance between two POIs to consider they match |
| `overlap` | a duration; optional | Minimum overlap between two POIs to consider they match |
| `train` | a file; required | Train dataset (POIs) |
| `test` | a file; required | Test dataset (POIs) |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `metrics` | a file | Metrics dataset |
{: class="table table-striped"}

## SpatialDistortion

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | a file; required | Train dataset |
| `test` | a file; required | Test dataset |
| `interpolate` | a boolean; optional; default: true | Whether to interpolate between points |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `metrics` | a file | Metrics dataset |
{: class="table table-striped"}

## SpatioTemporalDistortion

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | a file; required | Train dataset |
| `test` | a file; required | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `metrics` | a file | Metrics dataset |
{: class="table table-striped"}

