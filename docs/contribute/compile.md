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

## Compile from scratch

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

### 3. Building with Bazel

Once your environment is set up, you may want to build Accio.
Bazel is used to produce executable JAR files for Accio, which will appear in the `bazel-bin/` folder.
You can build the various components with the following commands:

```bash
bazel run @yarn//:yarn
bazel build accio/...
```

The first command is used to retrieve NPM dependencies (it is otherwise not done automatically, and the gateway will not compile), and the next command is used to compile all of Accio's components.

## Compile using Vagrant

If you have [Vagrant](https://www.vagrantup.com) and [VirtualBox](https://www.virtualbox.org) installed on your machine, you can launch a fully configured development environment via `vagrant up`.
More on this is described in the [dedicated section](../docs/vagrant.html).
The virtual machine that is created comes with all the needed tools to develop Accio.

The virtual machine is configured to the agent and the gateway processes as services.
They are installed as standard systemd services (named `accio-agent` and `accio-gateway`), and may as such be controlled by using the standard `systemctl` command.
For example, the agent can be restarted with the following command:
```bash
sudo systemctl restart accio-agent
```

Similarly, their logs can be accessed via the standard `journalctl` command.
For example, the logs of the agent can be retrieved with the following command:
```bash
sudo journalctl -u accio-agent
```

Inside the virtual machine, all the code repository is mounted inside the `/vagrant` folder.
However, to avoid altering files on your local computer, these files are later rsync'ed under `/home/vagrant/accio`, which is the actual workspace from which things will be built.
You can manually sync the files by launching the `update-sources` utility.
Note that some files are automatically excluded, such as `bazel-*` and `.git` directories.
The systemd files (found in the repository under `etc/vagrant/systemd`) will also be synchronised, but the services will *not* be restarted.

In addition to the standard `bazel` command, the `acciobuild` utility is also available to help you work in the Vagrant environment.
It automatically handles the synchronisation of sources, before actually calling Bazel and restarting the running components.
`acciobuild` takes as argument a list of components to build, where each component may be one of: agent, gateway, executor, client or all.
For example, the agent and the executor can be built with the following command:
```bash
acciobuild agent executor
```

Once the components are built, they are installed on the system, and the associated services are restarted (in the case of the agent and the gateway).

## Running tests

The following command is used to run all unit tests.

```bash
bazel test ...
```

The latter will compile all modules, independently on whether there are actually some tests associated with them, and then run all tests.
It means that all committed code must at least compile, even if not actually used by the system.
The test suit is launched automatically after each push by [Travis CI](https://travis-ci.org/privamov/accio).

## IDE integration

Bazel comes with an official plugin for [IntelliJ](https://plugins.jetbrains.com/plugin/8609-bazel), as well as [some non-official plugins](https://docs.bazel.build/versions/master/ide.html) for other IDEs (such as Eclipse).
