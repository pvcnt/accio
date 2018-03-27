---
layout: contribute
title: Compiling Accio
---

Accio has been successfully compiled on Mac OS X (10.10, 10.11 and 10.12) and Ubuntu (14.04 and 16.04).
This page describes how to successfully get started to develop Accio.
Accio can be built either entirely or component by component.
This page describes in depth the setup of a local development environment.
You can either use a [Vagrant-based environment](#using-vagrant) or create your environment [from scratch](#from-scratch).

* TOC
{:toc}

## Using Vagrant

If you have [Vagrant](https://www.vagrantup.com) and [VirtualBox](https://www.virtualbox.org) installed on your machine, you can launch a fully configured development environment via `vagrant up`.
More on this is described in the [dedicated section](../docs/vagrant.html).
The virtual machine that is created comes with all the needed tools to develop Accio.

## From scratch

If you prefer, you can also create a development environment from scratch, by installing everything needed.

### 1. Install requirements

The following dependencies must be met in order to compile Accio:

  * [Java JDK ≥ 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) is required to run both Bazel and Accio.
  * [Bazel ≥ 0.11.0](https://bazel.build) is required to build Accio.
  * [Node 3.9.5](https://nodejs.org//) is required if you want to develop Accio's web interface.
  * [Git](https://git-scm.com/) is needed to keep Accio's source code up-to-date.
  * Internet access is required to download dependencies both for Bazel and Accio.

Accio is compiled using the [Bazel](https://bazel.build) build tool.
The [Bazel documentation](https://docs.bazel.build/versions/master/install.html) contains more information about how to install it on various platforms (Linux, macOS and Windows are supported).

### 2. Clone the Git repository

The source code is hosted inside a Git repository  [on GitHub](https://github.com/privamov/accio).
You need to [install Git](https://git-scm.com/downloads) first, if it is not already done.
You can directly clone the repository to get the latest version of the source code.

```bash
git clone git@github.com:privamov/accio.git
cd accio
```

All the development is done on the `master` branch, and releases are tagged.
You can hence build a specific version:

```bash
git checkout v0.6.0 # Replace the version number with the one you target
```

## Building & running Accio

Once your environment is set up, you may want to build Accio.
Bazel is used to produce executable JAR files for Accio, which will appear in the `bazel-bin/` folder.
You can build the various components with the following commands:

```bash
bazel build accio/java/fr/cnrs/liris/accio/executor
bazel build accio/java/fr/cnrs/liris/accio/agent
bazel build accio/java/fr/cnrs/liris/accio/tools/cli
bazel build accio/java/fr/cnrs/liris/accio/gateway
```

In the Vagrant-based only environment, you have access to the `acciobuild` script.
It is used to build and restart specific components.
Those components are specified as a comma-separated list: agent, gateway, executor, client or all.

## Running tests

The following command is used to run all unit tests.

```bash
bazel test ...
```

The latter will compile all modules, independently on whether there are actually some tests associated with them, and then run all tests.
It means that all committed code must at least compile, even if not actually used by the system.
The test suit is launched automatically after each push by [Travis CI](https://travis-ci.org/privamov/accio).
