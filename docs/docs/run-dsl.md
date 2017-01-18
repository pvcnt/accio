---
layout: accio
nav: docs
title: Run definition
---

This section covers how to easily define runs as JSON documents.

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

### Glob

To define a parameter with a list of paths, you can use a JSON object whose only key is `glob`, mapped to a string written using the [glob syntax](https://en.wikipedia.org/wiki/Glob_(programming)#Syntax).
This exploration will generate one value per path matching your glob string.
Moreover, the glob string is subject to expansion for the `~` and `./` characters.

For example:

```json
{
  "params": {
    "files": {
      "glob": "/home/me/my_dataset/*.csv"
    }
  }
}
```

will produce a value per CSV file in the `/home/me/my_dataset` directory.

## Controlling randomness through a seed

Some workflows may include operators marked as *unstable*, which means they need some source of randomness when being executed.
This randomness is provided through a seed.
By default, a random seed is generated for each experiment, but you may fix it through the `seed` key.
If the workflow is not repeated, the same seed will be used for each run.
If the workflow is repeated, the seed will be used to deterministically produce a different seed for each run with the same combination of parameters.

## Validating experiments

Experiment files can be validated thanks to the `accio validate` command, which takes one or several files as arguments.
This command will not execute files, but only check there are correct and could be executed without any problem.
In case of an error, it will do its best to provide meaningful information to help you fix errors.
