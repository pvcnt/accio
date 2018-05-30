---
title: "Category: Lppm"
order: 50
---

* TOC
{:toc}

## GeoIndistinguishability

Generate locations satisfying geo-indistinguishability properties. The method used here is the one presented by the authors of the paper and consists in adding noise following a double-exponential distribution.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `epsilon` | a 64-bit float; optional; default: 0.001 | Privacy budget |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## Promesse

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `alpha` | a distance; required | Distance to enforce between two consecutive points |
| `data` | a file; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Transformed dataset |
{: class="table table-striped"}

## Wait4Me

Wrapper around the implementation of the Wait4Me algorithm provided by their authors.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `data` | a file; required | Input dataset |
| `k` | a 32-bit integer; required | Anonymity level |
| `delta` | a distance; required | Uncertainty |
| `radiusMax` | a distance; optional | Initial maximum radius used in clustering |
| `trashMax` | a 64-bit float; optional; default: 0.1 | Global maximum trash size, in percentage of the dataset size |
| `chunk` | a boolean; optional; default: false | Whether to chunk the input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | a file | Output dataset |
| `trashSize` | a 32-bit integer | Trash_size |
| `trashedPoints` | a 64-bit integer | Number of trashed points |
| `discernibility` | a 64-bit integer | Discernibility metric |
| `totalXyTranslations` | a distance | Total XY translations |
| `totalTimeTranslations` | a duration | Total time translations |
| `xyTranslationsCount` | a 32-bit integer | XY translation count |
| `timeTranslationsCount` | a 32-bit integer | Time translation count |
| `createdPoints` | a 32-bit integer | Number of created points |
| `deletedPoints` | a 32-bit integer | Number of deleted points |
| `meanSpatialTraceTranslation` | a distance | Mean spatial translation (per trace) |
| `meanTemporalTraceTranslation` | a duration | Mean temporal translation (per trace) |
| `meanSpatialPointTranslation` | a distance | Mean spatial translation (per point) |
| `meanTemporalPointTranslation` | a duration | Mean temporal translation (per point) |
{: class="table table-striped"}

