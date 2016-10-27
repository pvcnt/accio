---
layout: documentation
nav: docs
title: Testing Accio
---

Accio uses [Pants](http://pantsbuild.org) for running its test suit.
Before, make sure your development environment is ready and that you are able to [compile Accio](compiling.html).

* TOC
{:toc}

## Running unit tests

The following command can be used to run all unit tests.

```bash
$ ./pants test test/:: 
```

The later command will compile all modules for which there are some tests and run the associated tests.
You can also use the following command, which is the actual one being used by our continuous integration system.
The main difference is that in addition of running tests, all the modules will be compiled, independently on whether there are actually some tests associated with them.

```bash
$ ./pants test ::
```

## Continuous integration

The test suit is launched automatically after each push by [Travis CI](https://travis-ci.com/pvcnt/location-privacy).