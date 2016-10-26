---
layout: documentation
nav: docs
title: Installing Accio
---

* TOC
{:toc}

## Requirements

The following are required to run Accio:

  * Linux or Mac OS X.
  * Java 8.

## Using a binary distribution

The most straightforward way to install Accio is to fetch a binary release.

### Installing dependencies

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

### Running via a wrapper script

TODO.

### Running via a JAR

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

If you wish to develop Accio, or just live on the edge and use the latest available source code (possibly very unstable!), you can also [compile Accio from source](contribute/compiling.html).