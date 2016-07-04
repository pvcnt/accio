---
layout: documentation
nav: docs
title: Workflows & experiments definition
---

This section covers how to define workflows and experiments easily as JSON documents.

* TOC
{:toc}

## Workflow definition

A workflow is a JSON object formed of the following fields.

| Name | Description | Type | Required |
|:-----|:------------|:-----|:---------|
| name | A human-readable name. | string | false |
| owner | Person owning the workflow. It can include an email address between chevrons. | string | false |
| runs | Default number of runs for each node. It can be overriden on a per-node basis. | integer | false |
| graph | Nodes composing the workflow graph. The order in which nodes are defined does not matter. | object[] | true |
| graph[*].op | Operator name. | string | required |
| graph[*].name | Node name. By default it will be the operator's name. | string | optional |
| graph[*].inputs | Names of nodes acting as inputs of this node. | string[] | false |
| graph[*].params | Mapping between parameter names and values. All parameters without a default value should be specified. | object | false |
| graph[*].runs | Number of times this node should be ran. It is only useful for nodes producing metrics. If it is defined here, the value takes precedence over the global runs value. Results of different runs will be aggregated together (i.e., distributions resulting from all runs will be merged). | integer | false |
| graph[*].ephemeral | Force the node dataset output to be ephemeral (it can only be forced at he operator level). Ephemeral outputs are not stored as artifacts. | boolean | false |
{: class="table table-striped"}

Here is an example of a simple workflow's definition:

```json
{
  "name": "Geo-indistinguishability workflow",
  "owner": "John Doe <john.doe@gmail.com>",
  "graph": [
    {
      "op": "EventSource",
      "params": {
        "url": "/path/to/my/dataset"
      }
    },
    {
      "op": "GeoIndistinguishability",
      "inputs": ["EventSource"],
      "params": {
        "epsilon": "0.001"
      }
    },
    {
      "op": "PoisRetrieval",
      "name": "privacy",
      "inputs": ["EventSource", "GeoIndistinguishability"],
      "params": {
        "diameter": "200.meters",
        "duration": "15.minutes",
        "threshold": "100.meters"
      }
    },
    {
      "op": "SpatialDistortion",
      "name": "utility",
      "inputs": ["EventSource", "GeoIndistinguishability"]
    }
  ]
}
```

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

### Implicit conversion from a workflow to an experiment

Any workflow can be implicitly converted into an experiment whose goal is simply to run the workflow as-is.
You cannot run optimizations of explorations this way, but you can still define metadata through command line arguments (cf. [CLI interface](cli-interface.html) documentation).

## Parameters

### Parameters values

  * **integer, long and double:** JSON number.
  * **string:** JSON string.
  * **boolean:** JSON boolean.
  * **string list:** JSON array of strings.
  * **distance:** JSON string, formatted as `<quantity>.<unit>`, where `<quantity>` is a number and `<unit>` one of "meters", "kilometers" or "miles", either singular or plural.
  * **duration:** JSON string, formatted as `<quantity>.<unit>`, where `<quantity>` is a number and `<unit>` one of "millis", "seconds", "minutes", "hours" or "days", either singular or plural.
  * **timestamp:** JSON string, following [ISO 8601 format](https://www.w3.org/TR/NOTE-datetime), e.g., "2016-06-22T11:28:32Z".


### Parameters references

When you need to refer to a parameter, you have two options.
When inside the context of a node, you can use directly its name, for example `distance`.
When outside of the context of a node (e.g., when defining global parameters overrides), you must use its fully qualified name.
Fully qualified names are built with the node name, a slash '/' separator and the parameter name, e.g., `PoisRetrieval/distance`.