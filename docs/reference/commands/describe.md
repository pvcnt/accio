---
layout: docs
weight: 55
title: "Command: describe"
---

The `describe` command is used to show details about a specific resource.

## Usage
```
accio describe [<options>] <type> <identifier>
```

This command requires as arguments the type of resource to delete, followed by one or several identifiers.
Valid resource types are:

  * workflow
  * run
  * node (status of a node inside a run)
  * operator

Resource types can be used either singular or plural.

When used to inspect a run, this command returns information about the run and status of the nodes.
When used to inspect a node, this command returns information about the node as well as, if the node is completed, its outcome: metrics, error (if failed) or artifacts (if successful).
Please note that only a preview of the artifacts is shown, to keep the output readable.
If you want the full results, please use the [`export` command](export.md).

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens when the resource type is invalid or the resource does not exist.
* `5`: Internal error.

## Examples
```
# Get details about a specific run.
$ accio describe run 026f42d37ed14401bee76d67dc53ef1d

# Get status of the `PoisRetrieval` node of this run.
$ accio describe node e38283871cce42759279d459c8ed45ca/PoisRetrieval

# Get details about the `PoisRetrieval` operator.
$ accio describe operator PoisRetrieval
```
