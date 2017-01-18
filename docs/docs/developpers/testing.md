---
layout: docs
nav: docs
section: developpers
title: Testing Accio
---

Accio uses [Pants](http://pantsbuild.org) for running its test suit.
Before, make sure your development environment is ready and that you are able to [compile Accio](compiling.html).

## Running unit tests

The following command is used to run all unit tests.

```bash
$ ./pants test ::
```

The latter command will compile all modules, independently on whether there are actually some tests associated with them, and then run all available tests.
It means that all committed code must at least compile, even if not actually used by the system.

## Continuous integration

The test suit is launched automatically after each push by [Travis CI](https://travis-ci.org/privamov/accio).
