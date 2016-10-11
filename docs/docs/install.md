---
layout: documentation
nav: docs
title: Installing Accio
---

* TOC
{:toc}

## System requirements

The following are required to run Accio:

  * Platforms: Linux or Mac OS X;
  * Java: JDK 8 or later.

## Use a binary distribution

The most straightforward way to install Accio is to fetch a binary release.

### Install dependencies

Before running Accio, you need to install the Java runtime on your machine.
The exact procedure depends on the platform.

**Ubuntu Trusty (14.04 LTS).**
OpenJDK 8 is not available on Trusty.
To install Oracle JDK 8:

```bash
$ sudo add-apt-repository ppa:webupd8team/java
$ sudo apt-get update
$ sudo apt-get install oracle-java8-installer
```

Note: You might need to `sudo apt-get install software-properties-common` if you don't have the `add-apt-repository` command.
See [here](http://manpages.ubuntu.com/manpages/wily/man1/add-apt-repository.1.html).

**Ubuntu Wily (15.10).**
To install OpenJDK 8:

```bash
$ sudo apt-get install openjdk-8-jdk
```

**Mac OS X.**
JDK 8 can be downloaded from [Oracle's JDK Page](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
Look for "Mac OS X x64" under "Java SE Development Kit".
This will download a DMG image with an install wizard.

### Run via a wrapper script

TODO.

### Run via a JAR

You can also get the latest version of Accio as a pre-compiled JAR from [GitHub releases](https://github.com/pvcnt/location-privacy/releases).
You should then be able to run it the usual way:

```bash
$ java -jar accio.jar <command> <options>...
```

When running experiments, you may need to tune JVM memory options, like increasing the quantity of available memory:

```bash
$ java -Xmx8G -jar accio.jar <command> <options>...
```

## Compiling from source

You can also compile Accio by yourself from the source.
It is only needed if you want to be up-to-date with HEAD version, or want to develop Accio.
[Pants](http://pantsbuild.org) is the build tool used for this purpose.
To compile Accio from source, you will need to get the source from the Git repository and build it:

```bash
$ git clone git@github.com:pvcnt/location-privacy.git
$ cd location-privacy
$ ./pants binary src/jvm/fr/cnrs/liris/accio/cli:bin
```

If the compilation is successful, an `accio.jar` JAR will appear in the `dist/` folder.

To compile from source, you might need additional dependencies.
Alternatively, if you have [Vagrant](https://www.vagrantup.com) and [VirtualBox](https://www.virtualbox.org) installed on your machine, you can launch a development environment via `vagrant up`.
The source code will be located under `/vagrant`, with tools to compile and launch it already installed.