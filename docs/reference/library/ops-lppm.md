---
layout: docs
title: "Operators: Lppm"
weight: 50
---

* TOC
{:toc}

## GeoIndistinguishability

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

## Promesse

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `epsilon` | distance; required | Distance to enforce between two consecutive points |
| `data` | dataset; required | Input dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Output dataset |
{: class="table table-striped"}

## Wait4Me

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

