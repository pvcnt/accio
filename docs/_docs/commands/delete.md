---
title: "Command: delete"
---

The `delete` command is used to delete specific resources.

## Usage
```
accio delete [<options>] <type> <identifier> [<identifier>...]
```

This command requires as arguments the type of resource to delete, followed by one or several identifiers.
Valid resource types are:

  * run

Resource types can be used either singular or plural.

For runs, if a parent run is specified, all of its children will be deleted too.
For workflows, only those having no run associated with can be deleted.
All versions of this workflow will be deleted.

## Exit codes
* `0`: Success.
* `1`: Bad command-line, there was an error with the arguments/options/environment variables combination.
Notably happens when the resource type is invalid or at least one of the resources does not exist.
* `5`: Internal error.
