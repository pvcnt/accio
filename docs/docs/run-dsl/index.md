---
layout: docs
nav: docs
title: Run definition DSL
---

Runs are specified via a JSON DSL.
An interesting thing to note is that Accio comes with a built-in format to create parameter sweep.
What we call a run definition will actually generate one to several runs.
This way, you can very easily create an experiment that will launch thousands of runs, each one being a different combination of parameters of the same workflow.
All runs created this way will be *child runs*, and grouped together under a *parent run*.

Note that although workflows have to specified with their [appropriate DSL](workflow-dsl/index.html), runs can be created via the client without creating a run file.
However, this is considered as a good practice to store run definitions inside files, this gives better reproducibility if you want later to re-run the same experiment, and allows to version them (e.g., inside a Git repository.)

* TOC
{:toc}

## JSON schema
A run file should contain a single JSON object formed of the following fields.

| Name | Type | Description |
|:-----|:-----|:------------|
| workflow | string; required | Specification of the workflow to execute. |
| name | string; optional | A human-readable name. |
| notes | string; optional | Notes describing the purpose of this experiment. |
| tags | string[]; optional | Some tags, used when searching for runs. |
| seed | long; optional | Seed to be used for deterministic reproduction. If not specified, a random one will be generated. |
| repeat | integer; optional; default: 1 | Number of times to repeat each run. |
| params | object; optional | Mapping between parameter names and their values. |
{: class="table table-striped"}

Here is an example of a simple experiment definition.

```json
{
  "workflow": "my_awesome_workflow",
  "repeat": 3,
  "name": "My brand new experiment",
  "tags": ["brand", "new"],
  "params": {
    "epsilon": {
      "from": 0.00001,
      "to": 1,
      "log": true
    },
    "duration": {
      "value": "10.seconds"
    }
  }
}
```

## Specifying parameters
When defining an experiment, you can specify values for parameters.
You must specify a value for all parameters, except if they are only used by optional inputs.
You can either specify a single value or multiple values for any parameter, triggering the execution of multiple versions of the same original workflow.
Accio currently supports several ways to specify parameters, described in the next sections.

### Single value
To define a parameter with a single value, you can use a JSON object whose only key is `value`, mapped to the new value for the parameter.
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

Similarly to what can be done when specifying an input value as a constant, you can get a rid of the intermediate JSON object and map directly the single value to the parameter name.
The following code is equivalent to the previous one:

```json
{
  "params": {
    "epsilon": 0.001
  }
}
```

It may be needed sometimes to use the explicit form to disambiguate, especially if your input is a map.

### List of values
To define a parameter with a list of values, you can use a JSON object whose only key is `values`, mapped to a JSON array with all values taken by the parameter.
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
To define a parameter with a range of values, you can a JSON object with keys `from`, `to` and `step`, mapped respectively to the first value (inclusive) taken by the parameter, the last value (inclusive) taken by the parameter and the increment between two consecutive values.
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
For all other data types, the first, last and step values are all specified in the nominal data type.

You can also specify logarithmic progressions by setting one of the `log`, `log2` or `log10` keys (mutually exclusive) to `true`, to obtain a logarithmic progression respectively in base e, 2 or 10.
Only the following data types are supported for logarithmic progressions: double, distance and duration.
When following a logarithmic progression, the semantic of the parameters is the following:

1) a list of powers is generated from those parameters, after the logarithm has been applied on them
2) those powers are exponentiated in the appropriate base to produce values in the required range.

For example:

```json
{
  "params": {
    "epsilon": {
      "from": 10,
      "to": 10000,
      "step": 10,
      "log10": true
    }
  }
}
```

will produce values 10, 100, 1000 and 10000.

## Controlling randomness with a seed
Some workflows may include operators marked as *unstable*, which means they need some source of randomness when being executed.
This randomness is provided through a seed.
By default, a random seed is generated for each run, but you may fix it through the `seed` key.
If a run definition gives birth to several runs, the seed specified in the definition will be used to deterministically generate a seed for each child run.
If a run definition corresponds to a single run, the specified seed will be used directly.