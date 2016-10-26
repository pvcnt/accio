---
layout: documentation
nav: docs
title: Workflows definition
---

This section covers how to define workflows easily as JSON documents.

* TOC
{:toc}

## Workflow definition

A workflow is a JSON object formed of the following fields.

| Name | Description | Type | Required |
|:-----|:------------|:-----|:---------|
| name | A human-readable name. | string | false |
| owner | Person owning the workflow. It can include an email address between chevrons. | string | false |
| graph | Nodes composing the workflow graph. The order in which nodes are defined does not matter. | object[] | true |
| graph[*].op | Operator name. | string | required |
| graph[*].name | Node name. By default it will be the operator's name. | string | optional |
| graph[*].inputs | Mapping between input names and their values. All parameters without a default value should be specified. | object | false | 
{: class="table table-striped"}

Here is an example of a simple workflow's definition:

```json
{
  "name": "Geo-indistinguishability workflow",
  "owner": "John Doe <john.doe@gmail.com>",
  "graph": [
    {
      "op": "EventSource",
      "inputs": {
        "url": "/path/to/my/dataset"
      }
    },
    {
      "op": "GeoIndistinguishability",
      "inputs": {
        "epsilon": "0.001",
        "data": {
          "reference": "EventSource/data",
        }
      }
    },
    {
      "op": "PoisRetrieval",
      "name": "privacy",
      "inputs": {
        "diameter": "200.meters",
        "duration": "15.minutes",
        "threshold": "100.meters",
        "train": {
          "reference": "EventSource/data"
        },
        "test": {
          "reference": "GeoIndistinguishability/data"
        }
      }
    },
    {
      "op": "SpatialDistortion",
      "name": "utility",
      "inputs": {
        "train": {
          "reference": "EventSource/data"
        },
        "test": {
          "reference": "GeoIndistinguishability/data"
        }
      }
    }
  ]
}
```

## Inputs

Inputs can be either specified as constants or as references to some output of another node. 

### Input values

You can directly specify the value in the JSON for the following type.

  * **byte, short, integer, long:** JSON long number.
  * **double:** JSON float number.
  * **boolean:** JSON boolean.
  * **string:** JSON string.
  * **distance:** JSON string, formatted as `<quantity>.<unit>`, where `<quantity>` is a number and `<unit>` one of "meters", "kilometers" or "miles", either singular or plural.
  * **duration:** JSON string, formatted as `<quantity>.<unit>`, where `<quantity>` is a number and `<unit>` one of "millis", "seconds", "minutes", "hours" or "days", either singular or plural.
  * **timestamp:** JSON string, formatted with respect to[ISO 8601](https://www.w3.org/TR/NOTE-datetime), e.g., "2016-06-22T11:28:32Z".
  * **location:** JSON string, formatted as `<latitude>,<longitude>` where `<latitude>` and `<longitude>` are numbers.
  * **list:** JSON array of any other allowed type.
  * **set:** JSON array of any other allowed type.
  * **map:** JSON object of any other allowed type.

Please not that specifying directly the value is a shortcut.
Both forms are equivalent:

```json
{
  "op": "EventSource",
  "inputs": {
    "url": "/path/to/my/dataset"
  }
}
```
and:
```json
{
  "op": "EventSource",
  "inputs": {
    "url": {
      "value": "/path/to/my/dataset"
    }
  }
}
```
It may be needed sometimes to use the explicit form to disambiguate, especially if your input type is a map.

### Input references

Inputs can also be provided by the output of another node, in which case there will be a dependency between the two nodes.
You may reference another node by providing a JSON object with a `reference` key and the full name of the output as a value. 
```json
{
  "op": "GeoIndistinguishability",
  "inputs": {
    "data": {
      "reference": "EventSource/data"
    }
  }
}
```

References are formed of a node name and a port (an output port in this case) name, separated by a slash ('/').
Please be careful to specify the node name and not the operator case when the two are different.