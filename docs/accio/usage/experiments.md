---
layout: accio
nav: accio
title: Experiment definition
---

This section covers how to easily define experiments as JSON documents.

* TOC
{:toc}

## JSON schema

An experiment is a JSON object formed of the following fields.

| Name | Type | Description |
|:-----|:-----|:------------|
| workflow | string; required | Workflow to execute. |
| name | string; optional | A human-readable name. |
| notes | string; optional | Some free text notes. |
| tags | string[]; optional | Some tags, used when searching for experiments. |
| owner | string; optional | Person owning the experiment. It can include an email address between chevrons. |
| repeat | integer; optional; default: 1 | Number of times to repeat each run. |
| seed | long; optional | Initial seed to be used for deterministic reproduction of results. |
| params | object; optional | Mapping between parameter names and their values. |
{: class="table table-striped"}

Here is an example of a simple experiment definition.

```json
{
  "workflow": "./my-awesome-workflow.json",
  "runs": 3,
  "name": "My brand new experiment",
  "tags": ["brand", "new"],
  "params": {
    "epsilon": {
      "from": 0.00001,
      "to": 1,
      "log": true
    }
  }
}
```

## Specifying a workflow

The workflow to be executed is specified under the `workflow` key.
It is a path to a [workflow definition file](workflows.html), which can be either a relative (starting with `./`), home relative (starting with `~`) or absolute (starting with `/`) path.

## Specifying parameters

When defining an experiment, you can specify values for parameters.
You must specify a value for all parameters, except if they are only used by optional inputs.
You can either specify a single value or multiple values for any parameter, triggering the execution of multiple versions of the same original workflow.
Accio currently supports three ways to specify parameters: single value, list of values and range of values.

### Single value
 
To override a parameter with a single value, you have to use a JSON object whose only key is `value` mapped to the new value for the parameter.
The value should be specified using the same format as for [workflow input values](workflows.html#input-values).
For example:

```json
{
  "params": {
    "epsilon": {
      "value": 0.001
    }
  }
}
```

### List of values
 
To override a parameter with a list of values, you have to use a JSON object whose only key is `values` mapped to a JSON array with all values taken by the parameter.
The order of values has no importance.
Values should be specified using the same format as for [workflow input values](workflows.html#input-values).
For example:

```json
{
  "params": {
    "epsilon": {
      "values": [1, 0.1, 0.001, 0.0001]
    }
  }
}
```

### Range of values
 
To override a parameter with a range of values, you have to use a JSON object with keys `from`, `to` and `step`, mapped respectively to the first value (inclusive) taken by the parameter, the last value (inclusive) taken by the parameter and the increment between two consecutive values.
The first value (under the `from` key) must be lower than the last value (under the `to` key). 
Values should be specified using the same format as for [workflow input values](workflows.html#input-values).
For example:

```json
{
  "params": {
    "epsilon": {
      "from": 0.0001,
      "to": 1
    }
  }
}
```

Not all data types may be specified as a range of values.
Only the following data types are supported: byte, short, integer, long, double, distance, duration, timestamp.
For the timestamp data type, the first and last values are specified as timestamps, whereas the step between consecutive values is specified as a duration.
For all other data types, the first, last and step values are all specified in the nomminal data type.

You can also specify logarithmic progressions by setting one of the `log`, `log2` or `log10` keys (mutually exclusive) to `true`, to obtain a logarithmic progression respectively in base e, 2 or 10.