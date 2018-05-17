---
title: Installing Accio
order: 30
---

The most common case is that you have an Accio cluster already installed by your system administrator on some machines, that you want to interact with.
In this case, you only need to install a client to communicate with this cluster.
**We recommend installing Accio from a binary release**, as it is the easiest and more robust method.
Compiling from source should be reserved for those who want to live on the edge.

* TOC
{:toc}  

These instructions are for installing a client communicating with an existing Accio cluster.
Instructions about how to deploy an Accio cluster can be found [in the appropriate section](../deploy/).

## Installing from a release

This section explains how to install the Accio client from a binary release.
It is by far the easiest and preferred option.

###  1. Install requirements

The only requirement to run Accio is to have a JRE for Java 8.
Before running Accio, you hence need to install the Java runtime on your machine.
Please that though you only need a JRE (Java Runtime Environment) to execute Accio, you may want to install a JDK (Java Development Kit) instead;
it is perfectly fine, as the JDK includes the JRE.

The instructions differ depending on your operating system.

**Ubuntu Trusty (14.04 LTS).**
OpenJDK 8 is not available on Trusty (if you install the JRE through your package manager, it will install Java 7).
To install Oracle JRE 8, you need to first add a PPA repository.

```bash
sudo add-apt-repository ppa:openjdk-r/ppa
sudo apt-get update
sudo apt-get install openjdk-8-jre
```

Note: You might need to install the `software-properties-common` package if you don't have the `add-apt-repository` command.

```bash
sudo apt-get install software-properties-common
```

**Ubuntu Wily (15.10) and later.**
OpenJDK 8 is packaged with recent versions of Ubuntu, and can be installed straight from the package manager.

```bash
sudo apt-get install openjdk-8-jre
```

**Windows.**
Java 8 JRE can be downloaded from [Oracle's JRE Page](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html).
Look for "Windows x64 Offline" under "Java SE Runtime Environment".
This will download an executable file with an install wizard.

**macOS.**
Java 8 JRE can be downloaded from [Oracle's JRE Page](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html).
Look for "Mac OS X" under "Java SE Runtime Environment".
This will download a DMG image with an install wizard.


### 2. Download binary

Download the `accio` file from [the latest release](https://github.com/privamov/accio/releases/latest).
This file is actually a simple bash wrapper around an embedded JAR.
You need to give it the executable permissions before executing it.

```bash
curl -L -o accio https://github.com/privamov/accio/releases/download/v0.6.0/accio
chmod +x accio
./accio version -client
```

The file you just downloaded is directly executable by your bash.
However, you may want to move it under a location that is in your PATH, thus allowing you to use it from any directory.
Depending on your operating system, it may also be possible to move the binary under a system-wide directory where it can be share by several users.

### 4. Configuration

For now, you can only use a very restricted set of commands, because we have not yet configured our client how to communicate with the Accio cluster.
Configuration is done inside a `.accio/clusters.json` file under your home directory.
You can define multiple clusters, each one having at least a name and an address where to contact it.
The first cluster ever is the default cluster, that is used if none is explicitly given.

A simple configuration file looks like this:

```json
[{
  "name": "default",
  "server": "192.168.50.4:9999"
}]
```

Make sure the specified address matches the one of your actual cluster.
More detailed information about the client configuration may be found on [the dedicated page](configuration.html).

## Compile from source

This section explains how to compile the Accio client from the source code.

### 1. Install requirements

The following dependencies must be met in order to build Accio:

  * [Java JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) is required to run both Bazel and Accio.
  * [Bazel](https://bazel.build) is required to build Accio.
  * [Git](https://git-scm.com/) is needed to keep Accio's source code up-to-date.
  * Internet access is required to download dependencies both for Bazel and Accio.

Bazel is our build tool of choice.
Accio and Bazel require at least Java 8 (Accio only needs a JRE, but Bazel may need a JDK).
[Bazel installation documentation](https://docs.bazel.build/versions/master/install.html) contains more information about how to install it on various platforms (Linux, macOS and Windows are supported).
Accio requires at least Bazel 0.11.0.

### 2. Clone the Git repository

The source code is hosted inside a Git repository [on GitHub](https://github.com/privamov/accio).
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

### 3. Build the client

You can now build the Accio client from source.

```bash
bazel build accio/java/fr/cnrs/liris/accio/cli:cli_deploy.jar
```

This will create a "fat JAR" under `bazel-bin/accio/java/fr/cnrs/liris/accio/cli/cli_deploy.jar`.
It can be executed wherever a Java JRE is available, for example:

```bash
java -jar bazel-bin/accio/java/fr/cnrs/liris/accio/cli/cli_deploy.jar version -client
```

The section for contributors contains full instructions [how to compile and develop Accio](../contribute/compile.html).
{: .note}

## Local Vagrant cluster

If you want to quickly bootstrap a local cluster, or to develop Accio, you can also use the [Vagrant](https://www.vagrantup.com/) distribution.
Vagrant is a tool to easily create virtual machines on any operating system.
Using Vagrant allows to create a full cluster contained inside a virtual machine, useful to test all features without having a live cluster.
It is also the preferred way for developers to contribute to Accio.
This is however *not recommended for production usage*.

  * [Running a local environment with Vagrant](vagrant.html)
