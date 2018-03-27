---
layout: docs
weight: 95
title: "Command: version"
---

The `version` command is used to get client and server build information.

## Usage
```
accio version [<options>]
```

This command requires no argument.
It prints the current version of the Accio client binary and of the Accio cluster.
As they are installed and upgraded independently, these two versions can possibly be different.

## Options
* `-client`: Display only the version of the client (no server required).

## Exit codes
* `0`: Success.
* `5`: Internal error.

## Examples
```
$ accio version
Server version: 0.6.0
Client version: 0.6.0
```
