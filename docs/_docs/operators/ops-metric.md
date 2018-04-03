---
title: "Category: Metric"
order: 51
---

* TOC
{:toc}

## AreaCoverage

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

## CountQueriesDistortion

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

## DataCompleteness

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | dataset; required | Train dataset |
| `test` | dataset; required | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `value` | map(string, double) | Data completeness |
{: class="table table-striped"}

## HeatMapDistortion

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

## MmcReident

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | dataset; required | Input train dataset |
| `test` | dataset; required | Input test dataset |
| `minPts` | integer; optional; default: 10 | Clustering parameter : minimum points in a cluster |
| `diameter` | distance; optional; default: 3000.0.meters | Clustering parameter : maximum size cluster |
| `duration` | duration; optional; default: 3.seconds+600.milliseconds | Clustering parameter : maximum cluster duration |
| `attack` | string; optional; default: gambs | Attack |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `matches` | map(string, string) | Matches between users |
| `rate` | double | Re-Ident rate |
{: class="table table-striped"}

## PoisReident

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | dataset; required | Train dataset (POIs) |
| `test` | dataset; required | Test dataset (POIs) |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `matches` | map(string, string) | Matches between users from test and train datasets |
| `rate` | double | Correct re-identifications rate |
{: class="table table-striped"}

## PoisRetrieval

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

## SpatialDistortion

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

## SpatioTemporalDistortion

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

## TransmissionDelay

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `train` | dataset; required | Train dataset |
| `test` | dataset; required | Test dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `value` | map(string, long) | Transmission delay |
{: class="table table-striped"}

