---
layout: docs
weight: 60
title: "Command: get"
---

The `get` command displays a list of resources.

## Usage
```
accio get [<options>] type
```

This command accepts a single argument, the type of resource to display.
Valid resource types are:

  * workflow
  * run
  * operator

Resource types can be used either singular or plural.
By default it displays only active resources (e.g., runs being executed), but the `-all` option can override this behavior.
Resources are returned in reverse chronological order (when applicable), the most recently created one being the first result.

## Options
* `-all`: Display all resources, including inactive ones.
* `-owner=<string>`: Display only resources belonging to a given owner (specified by his exact username).
* `-tags=<string>[,<string>...]`: Display only resources having all of the given tags.
* `-n=<integer>`: Limit the number of displayed results.
By default all resources are displayed.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens if resource type is invalid.
* `5`: Internal error.

## Examples
```
# List all active runs.
$ accio get runs

# List 5 last runs of John Doe.
$ accio get runs -all -n=5 -owner=jdoe

# List available operators in this cluster.
$ accio get operators
```
