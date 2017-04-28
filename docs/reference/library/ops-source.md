---
layout: docs
title: "Operators: Source"
weight: 52
---

* TOC
{:toc}

## EventSource

This operator can manipulate the source dataset, essentially to reduce its size, through some basic preprocessing.

| Input name | Type | Description |
|:-----------|:-----|:------------|
| `url` | string; required | Dataset URL |
| `kind` | string; optional; default: csv | Kind of dataset |
{: class="table table-striped"}

| Output name | Type | Description |
|:------------|:-----|:------------|
| `data` | dataset | Source dataset |
{: class="table table-striped"}

