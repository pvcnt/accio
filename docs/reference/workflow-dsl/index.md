---
layout: docs
weight: 20
title: Workflow definition DSL
---

This page provides the full specification of the JSON-based DSL used to define workflows.

<div class="alert alert-info" markdown="1">
  :book: For a more progressive introduction to workflows, please refer to [this guide](../../user-guide/authoring/workflows.html).
</div>

* TOC
{:toc}

## JSON schema

A workflow file should contain a single JSON object formed of the following fields.

| Name | Type | Description |
|:-----|:-----|:------------|
| id | string; optional | Unique identifier. If not specified, it will be set to the filename (without its extension). Must match `[a-zA-Z][a-zA-Z0-9_.-]+`. |
| name | string; optional | Human-readable name. |
| owner | string; optional | Person owning the workflow. It can include an email address between chevrons. |
| params | object[]; optional | Workflow parameters. The order in which they are defined does not matter. |
| params[].name | string; required | Parameter name. Must match `[a-z][a-zA-Z0-9_]+`. |
| params[].kind | string; required | Parameter data type (see below). |
| params[].default_value | any; optional | Default value. |
| graph | object[]; required | Nodes composing the workflow graph. The order in which they are defined does not matter. |
| graph[].op | string; required | Operator name. |
| graph[].name | string; optional | Node name. By default it will be the operator's name. Must match `[A-Z][a-zA-Z0-9_]+`. |
| graph[].inputs | object; optional | Mapping between input names and their values. All parameters without a default value should be specified. |
{: class="table table-striped"}

This workflow is formed of four nodes, connected together.
`EventSource` is the only root node (it has no dependency to another node), while `Privacy` and `Utility` are leaf nodes (no other node depend on them).
The workflow `id` is a unique identifier that should uniquely identify it.
If none is specified, it will by default be the file name, without its extension (e.g., workflow defined in `/path/to/my_workflow.json` will have `my_workflow` as its default identifier).
The workflow `name` has no constraint, it is only used for information purposes.

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
Values are specified differently depending on their type, following the specification in [this table](#data-types).
For example:

```json
{
  "op": "EventSource",
  "inputs": {
    "url": {"value": "/path/to/my/dataset"}
  }
}
```

As a shortcut when specifying an input as a constant, you can get a rid of the intermediate JSON object and map directly the constant to the input name.
The following code is equivalent to the previous one:

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
Parameters must be explicitly declared under the workflow `params` field, and their data type must be consistent with those of the ports they are used in.

```json
{
  "op": "GeoIndistinguishability",
  "inputs": {
    "epsilon": {"param": "epsilon"},
    "data": {"reference": "EventSource/data"}
  }
}
```

### Unspecified
Under some conditions, you can leave an input unspecified:

  * if the operator defines a default value for this port; or
  * if the operator defines this port as optional, which means it can still be executed if no value at all is specified.

If one of those conditions is satisfied, you do not have to specify a value for this input.
Otherwise, you should fill it.

## Specifying workflow parameters
Workflow parameters allow to define parametric workflows, that can be customized at runtime.
Parameters must be explicitly declared under the workflow `params` field, they have a name, a data type and possibly a default value.
They can only be used in ports of the same data type (e.g., you cannot use a parameter on an integer port and then on a distance port).
Parameter names are global to a workflow and therefore not namespaced.

Normally, all parameters should be specified when launching a workflow.
However, under some conditions, they can be left unspecified:

  * if a default value is specified in the workflow definition; or
  * if all ports using this parameter [could be left unspecified](#unspecified).

Once a value for a parameter is specified, it will override any default value, whether it comes from the parameter or the input port.
It means that when using parameters, there are three possible sources for the actual port value, in order of precedence:

  * the parameter value, specified when launching the workflow;
  * the parameter default value, specified when creating the workflow;
  * the input port default value, specified when creating the operator.

## Data types
Accio comes with types that are strongly enforced.
Here is a list of supported types, with their names (as they should be specified in any field requiring a data type) and the way to format their values in JSON.

| Name | Collection | JSON format of value |
|:-----|:-----------|:---------------------|
| byte | | JSON number. |
| integer | | JSON number. |
| long | | JSON number. |
| double | | JSON number. |
| boolean | | JSON boolean. |
| string | | JSON string. |
| distance | | JSON string, formatted as `<quantity>.<unit>`, where `<quantity>` is a number and `<unit>` one of "meters", "kilometers" or "miles", either singular or plural. |
| duration | | JSON string, formatted as `<quantity>.<unit>`, where `<quantity>` is a number and `<unit>` one of "millis", "seconds", "minutes", "hours" or "days", either singular or plural. |
| timestamp | | JSON string, formatted with respect to [ISO 8601](https://www.w3.org/TR/NOTE-datetime), e.g., "2016-06-22T11:28:32Z". |
| location | | JSON string, formatted as `<latitude>,<longitude>` where `<latitude>` and `<longitude>` are numbers and expressed in degrees. |
| dataset | | JSON string, specifying the dataset URI. |
| list | :heavy_check_mark: | JSON array of any other allowed type. |
| set | :heavy_check_mark: | JSON array of any other allowed type. |
| map | :heavy_check_mark: | JSON object of any other allowed type. |
{: class="table table-striped"}
