---
layout: docs
nav: docs
title: Compiling Accio
---

Accio has been successfully compiled on Mac OS X (10.10, 10.11 and 10.12) and Ubuntu (14.04 and 16.04).
This page describes how to successfully get started to develop Accio.
Accio can be built either entirely or component by component.

This page describes in depth the setup of a local development environment.
Alternatively, if you have [Vagrant](https://www.vagrantup.com) and [VirtualBox](https://www.virtualbox.org) installed on your machine, you can launch a fully configured development environment via `vagrant up`.
It is perfectly suited for developping Accio.
More on this is described in the [getting started section](../getting-started/index.html).

## Requirements
The following dependencies must be met in order to compile Accio:

  * [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) is required to run both Pants and Accio.
  * [Python 2.7.x](https://www.python.org/) and its development headers are required by Pants (*not* Python 3.x).
  * [gcc/G++](https://gcc.gnu.org/) is required by Pants.
  * [Node 3.9.5](https://nodejs.org/en/) is required if you want to develop Accio's web interface.
  * [Git](https://git-scm.com/) is needed to keep Accio's source code up-to-date.
  * Internet access is required to download dependencies both for Pants and Accio.

## Getting the source code
The source code is hosted inside a Git repository  [on GitHub](https://github.com/privamov/accio).
You can directly clone the repository to get the latest version of the source code.

```bash
$ git clone git@github.com:privamov/accio.git
```

We recommend avoiding cloning the repository in a folder whose path contains special characters such as accents.
It may cause Pants to fail.
All the development is done on the `master` branch, and releases are tagged.

## Installing Pants
Accio is compiled using the [Pants](http://pantsbuild.org) build tool.
Pants is usually not installed globally but rather once per repository.
If all requirements are fulfilled, you should be able to bootstrap Pants automatically by calling it from the sources root.

```bash
$ cd /path/to/accio
$ ./pants -V
1.2.0
```

In most settings this command should be sufficient.
If you need more help about Pants, you can read the appropriate [installation instructions](http://www.pantsbuild.org/install.html).

## Building & running Accio
Pants is used to produce executable JAR files for Accio, which will appear in the `dist/` folder.
For example, you can build the client executable using the following command.

```bash
$ ./pants binary src/jvm/fr/cnrs/liris/accio/client:bin
```

If the compilation is successful, an `accio-client.jar` JAR will appear in the `dist/` folder.
When developing, it can also be convinient to run Accio directly, which involves recompiling changed files on-the-fly.

```bash
$ ./pants run src/jvm/fr/cnrs/liris/accio/client:bin -- command line arguments to accio
```

## Running tests
The following command is used to run all unit tests.

```bash
$ ./pants test ::
```

The latter will compile all modules, independently on whether there are actually some tests associated with them, and then run all tests.
It means that all committed code must at least compile, even if not actually used by the system.