---
layout: documentation
nav: docs
title: Workflows & experiments definition
---

This section covers how to define experiments easily as JSON documents.

* TOC
{:toc}

## Experiment definition

An experiment is a JSON object formed of the following fields.

| Name | Description | Type | Required |
|:-----|:------------|:-----|:---------|
| name | A human-readable name. | string | false |
| notes | Some free text notes. | string | false |
| tags | Some tags. | string[] |  false |
| workflow | Path to a workflow definition file. It can be either a relative (starting with `./`), home relative (starting with `~`) or absolute (starting with `/`) path. | string | true |
| runs | Override the default number of runs for each node. | integer | false |
| params | Mapping between fully qualified names of parameters to override and new values. | object | false|
| exploration | Exploration configuration. | object | false |
| exploration.params | Mapping between fully qualified names of parameters to override and domains of values. | object | true |
| optimization | Optimization configuration. | object | false |
| optimization.grid | Mapping between fully qualified names of parameters to override and domains of values. | object | true |
| optimization.iters | Number of steps per temperature value. | integer | false |
| optimization.contraction | Contraction of the domain when choosing a new value. | double | false |
| optimization.objectives | Optimization objectives. | object[] | true |
| optimization.objectives[*].type | Objective type: "minimize" or "optimize". | string | true |
| optimization.objectives[*].metric | Objective metric. It is the fully qualified name of an artifact. | string | true |
| optimization.objectives[*].threshold | Objective threshold. | double | false |
{: class="table table-striped"}

Here is an example of a simple experiment definition:

```json
{
  "workflow": "./geoind-workflow.json",
  "runs": 3,
  "name": "My brand new experiment",
  "tags": ["brand", "new"],
  "optimization": {
    "grid": {
      "GeoIndistinguishability/epsilon": {
        "ranges": [[1,0.01,0.001], [0.01,0.001,0.00001]]
      }
    },
    "objectives": [
      {
        "type": "minimize",
        "metric": "privacy/fscore"
      },
      {
        "type": "minimize",
        "metric": "utility/avg",
        "threshold": 500
      }
    ]
  }
}
```

## Parameters

### Parameters values

  * **integer:** JSON integer number.
  * **long:** JSON long number.
  * **double:** JSON float number.
  * **string:** JSON string.
  * **boolean:** JSON boolean.
  * **string list:** JSON array of strings.
  * **distance:** JSON string, formatted as `<quantity>.<unit>`, where `<quantity>` is a number and `<unit>` one of "meters", "kilometers" or "miles", either singular or plural.
  * **duration:** JSON string, formatted as `<quantity>.<unit>`, where `<quantity>` is a number and `<unit>` one of "millis", "seconds", "minutes", "hours" or "days", either singular or plural.
  * **timestamp:** JSON string, formatted with respect to[ISO 8601](https://www.w3.org/TR/NOTE-datetime), e.g., "2016-06-22T11:28:32Z".

### Parameters ranges

When defining an exploration, you can define ranges of parameters to explore.

  * **integer:** JSON array, either `[from, to, step]` or `[from, to]` (in the latter the step is assumed to be 1). Elements are JSON integers. Boundaries are inclusive.
  * **long:** JSON array, either `[from, to, step]` or `[from, to]` (in the latter the step is assumed to be 1). Elements are JSON longs. Boundaries are inclusive.
  * **double:** JSON array, `[from, to, step]`. Elements are JSON floats. Boundaries are inclusive.
  * **distance:** JSON array, `[from, to, step]`. Elements are JSON strings formatted as distances. Boundaries are inclusive.
  * **duration:** JSON array, `[from, to, step]`. Elements are JSON strings formatted as durations. Boundaries are inclusive.
  * **timestamp:** JSON array, `[from, to, step]`. `from` and `to` are JSON strings formatted as timetamps, `step` is a string formatted as a duration. Boundaries are inclusive.
  * **string:** not applicable.
  * **boolean:** not applicable.
  * **string list:** not applicable.

### Parameters references

When you need to refer to a parameter, you have two options.
When inside the context of a node, you can use directly its name, for example `distance`.
When outside of the context of a node (e.g., when defining global parameters overrides), you must use its fully qualified name.
Fully qualified names are built with the node name, a slash '/' separator and the parameter name, e.g., `PoisRetrieval/distance`.