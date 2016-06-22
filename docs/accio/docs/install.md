---
layout: page
title: Installing Accio
---

## System requirements

Supported platforms:

  * Linux
  * Mac OS

Java:

  * Java JDK 8 or later

## Install dependencies

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

## Install Accio from a wrapper script

## Install Accio from a JAR

You can also get the latest version of Accio as a pre-compiled JAR from [GitHub](https://github.com/pvcnt/location-privacy/releases).
You should then be able to run it the usual way:

```bash
$ java -jar accio.jar <command> <options>...
```

When running experiments, you will need to tune JVM memory options:

```bash
$ java -Xmx8G -jar accio.jar <command> <options>...
```

## Compiling from source
If you want to compile Accio from source, you will need to get the source from GitHub and built it:

```bash
$ git clone git@github.com:pvcnt/location-privacy.git
$ cd location-privacy
$ ./pants binary src/jvm/fr/cnrs/liris/accio/cli:bin
```

If the compilation is successful, an `accio.jar` will appear in the `dist/` folder.

To compile from source, you might need additional dependencies.
Alternatively, if you have [Vagrant](https://www.vagrantup.com) and [VirtualBox](https://www.virtualbox.org) installed on your machine, you can launch a development environment via `vagrant up`.
The source code will be located under `/vagrant`, with tools to compile and launch it already installed.