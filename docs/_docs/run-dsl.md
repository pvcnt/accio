---
title: Run definition DSL
category: Reference
order: 10
---

This page provides the full specification of the JSON-based DSL used to define runs.
For a more progressive introduction to workflows, please refer to [this guide](creating-runs.html).

## JSON schema

A run file should contain a single JSON object formed of the following fields.

| Name | Type | Description |
|:-----|:-----|:------------|
| workflow | string | Specification of the workflow to execute. |
| name | string; optional | A human-readable name. |
| notes | string; optional | Notes describing the purpose of this experiment. |
| tags | string[]; optional | Some tags, used when searching for runs. |
| seed | long; optional | Seed to be used for deterministic reproduction. If not specified, a random one will be generated. |
| repeat | integer; optional; default: 1 | Number of times to repeat each run. |
| params | object; optional | Mapping between parameter names and their values. |
{: .table .table-striped}

## Specifying parameters

When defining an experiment, you can specify values for parameters.
You must specify a value for all parameters, except if they are only used by optional inputs.
You can either specify a single value or multiple values for any parameter, triggering the execution of multiple versions of the same original workflow.
Accio currently supports several ways to specify parameters, described in the next sections.

The different values that a parameter will take are defined via a JSON object whose only key is `values`, mapped to a JSON array with all values taken by the parameter.
The order of values has no importance.
Values should be specified using the same format as for [workflow input values](creating-workflows.html).
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

Of course, a singleton can be passed as `values`.
If at least one parameter has more than one possible value, the cross product of all values will be taken to determine the runs to actually trigger.
For example, 8 (4 x 2 x 1) different runs will be launched with the following configuration:

```json
{
  "params": {
    "epsilon": {
      "values": [1, 0.1, 0.001, 0.0001]
    },
    "uri": {
      "values": ["/path/to/geolife", "/path/to/cabspotting"]    
    },
    "level": {
      "values": [12]
    }
  }
}
```


## Controlling randomness with a seed

Some workflows may include operators marked as *unstable*, which means they need some source of randomness when being executed.
This randomness is provided through a seed.
By default, a random seed is generated for each run, but you may fix it through the `seed` key.
If a run definition gives birth to several runs, the seed specified in the definition will be used to deterministically generate a seed for each child run.
If a run definition corresponds to a single run, the specified seed will be used directly.
