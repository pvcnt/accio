---
layout: accio
nav: accio
title: Workflow definition
---

This section covers how to easily define workflows as JSON documents.

* TOC
{:toc}

## JSON schema

A workflow is a JSON object formed of the following fields.

| Name | Type | Description |
|:-----|:-----|:------------|
| name | string; optional | A human-readable name. |
| owner | string; optional | Person owning the workflow. It can include an email address between chevrons. |
| graph | object[]; required | Nodes composing the workflow graph. The order in which nodes are defined does not matter. |
| graph[*].op | string; required | Operator name. |
| graph[*].name | string; optional | Node name. By default it will be the operator's name. |
| graph[*].inputs | object; optional | Mapping between input names and their values. All parameters without a default value should be specified. | 
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
        "url": {"value": "/path/to/my/dataset"}
      }
    },
    {
      "op": "GeoIndistinguishability",
      "inputs": {
        "epsilon": {"param": "epsilon"},
        "data": {"reference": "EventSource/data"}
      }
    },
    {
      "op": "PoisRetrieval",
      "name": "Privacy",
      "inputs": {
        "diameter": {"value": "200.meters"},
        "duration": {"value": "15.minutes"},
        "threshold": {"value": "100.meters"},
        "train": {"reference": "EventSource/data"},
        "test": {"reference": "GeoIndistinguishability/data"}
      }
    },
    {
      "op": "SpatialDistortion",
      "name": "Utility",
      "inputs": {
        "train": {"reference": "EventSource/data"},
        "test": {"reference": "GeoIndistinguishability/data"}
      }
    }
  ]
}
```

This workflow is formed of four nodes, connected together.
`EventSource` is the only root node (it has no dependency to another node), while `Privacy` and `Utility` are leaf nodes (no other node depend on them).

## Specifying nodes

Nodes are specified under the `graph` key in no particular order.
A node is a particular instantiation of an operator (i.e., with well-defined inputs).
By default, a node is named after its operator name, but this name can be overriden with the `name` key.
An operator can be instantiated more than once, in which case you have to give a name to subsequent operators (there cannot be two nodes with the same name).
All nodes have to form a directed acyclic graph (DAG), which will be enforced at runtime when running a workflow. 

## Specifying inputs

An input can be either specified as a constant value, as a reference to the output of another node or as a reference to a parameter. 

### Constant value

You may directly specify the value of an input as a constant by providing a JSON object with a `value` key and the constant as a value.
Values are specified differently depending on their type.

| Data type | JSON format |
|:----------|:------------|
| byte | JSON number. |
| short | JSON number. |
| integer | JSON number. |
| long | JSON number. |
| double | JSON number. |
| boolean | JSON boolean. |
| string | JSON string. |
| distance | JSON string, formatted as `<quantity>.<unit>`, where `<quantity>` is a number and `<unit>` one of "meters", "kilometers" or "miles", either singular or plural. |
| duration | JSON string, formatted as `<quantity>.<unit>`, where `<quantity>` is a number and `<unit>` one of "millis", "seconds", "minutes", "hours" or "days", either singular or plural. |
| timestamp | JSON string, formatted with respect to [ISO 8601](https://www.w3.org/TR/NOTE-datetime), e.g., "2016-06-22T11:28:32Z". |
| location | JSON string, formatted as `<latitude>,<longitude>` where `<latitude>` and `<longitude>` are numbers and expressed in degrees. |
| list | JSON array of any other allowed type. |
| set | JSON array of any other allowed type. |
| map | JSON object of any other allowed type. |
| dataset | Cannot be specified as JSON. |
| image | Cannot be specified as JSON. |
{: class="table table-striped"}

There is a shortcut when specifying an input as a constant.
You can get a rid of the intermediate JSON object and map directly the constant to the input name.
Both forms are equivalent:

```json
{
  "op": "EventSource",
  "inputs": {
    "url": {"value": "/path/to/my/dataset"}
  }
}
```

and:

```json
{
  "op": "EventSource",
  "inputs": {
    "url": "/path/to/my/dataset"
  }
}
```

It may be needed sometimes to use the explicit form to disambiguate, especially if your input is a map.

### Reference

Inputs can be filled by the output of another node, in which case there will be a dependency between the two nodes.
You may reference another node by providing a JSON object with a `reference` key and the full name of the output as a value.
 
```json
{
  "op": "GeoIndistinguishability",
  "inputs": {
    "data": {"reference": "EventSource/data"}
  }
}
```

References are formed of a node name and an output port name, separated by a slash ('/').
Please be careful to specify the node name and not the operator name when the two are different.

### Parameter

Inputs can be filled by the value of a workflow parameter.
Parameters are useful to let the user vary some parameters are runtime, possibly affecting multiple ports at once.
You may reference a parameter by providing a JSON object with a `param` key and the name of the parameter as a value.
You do not need to explicitly create parameters before using them.
However, you must ensure that a given parameter is used only on ports of the same data type (e.g., you cannot use a parameter on an integer port and then on a distance port).
Parameter names are global to a workflow and therefore not namespaced.

```json
{
  "op": "GeoIndistinguishability",
  "inputs": {
    "epsilon": {"param": "epsilon"},
    "data": {"reference": "EventSource/data"}
  }
}
```

You may also define a default value for the input port, under the `default_value` key.
This way, if the parameter is not filled when launching the workflow, this default value will be used.
It means when using a parameter, you have three possible sources for the actual port value, in order of precedence:

  * the parameter value, specified when launching the workflow;
  * the parameter default value, specified when creating the workflow;
  * the port default value, specified when creating the operator.

Please note that despite the fact parameters are global, the parameter default value is specified on a per-port basis.
It means that until the parameter value is explicitly specified when launching the workflow, ports depending on the same parameter may have different values (depending on the parameter or operator default values).