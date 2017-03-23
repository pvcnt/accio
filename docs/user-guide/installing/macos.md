---
layout: docs
weight: 20
title: Installing on Mac OS
---

This page explains how to install the Accio client on Mac OS.

## 1. Install Java
The only requirement to run Accio is to have a JRE for Java 8.
Before running Accio, you need to install the Java runtime on your machine.
Please that though you only need a JRE (Java Runtime Environment) to execute Accio, you may want to install a JDK (Java Development Kit) instead;
it is perfectly fine, as the JDK includes the JRE.

Java 8 JRE can be downloaded from [Oracle's JRE Page](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html).
Look for "Mac OS X" under "Java SE Runtime Environment".
This will download a DMG image with an install wizard.

## 2. Download
Download the `accio` file from [the latest release](https://github.com/privamov/accio/releases/latest).
This file is actually a simple bash wrapper around an embedded JAR.
You need to give it the executable permissions before executing it.

```
$ curl -L -o accio https://github.com/privamov/accio/releases/download/v0.5.0/accio
$ chmod +x accio
$ ./accio version -client
Client version: 0.5.0
```

## 3. Installation (optional)
The file you just downloaded is directly launchable by the JRE, as we have seen.
However, you may want to move it under a location that is in your PATH, thus allowing you to use it from any directory.
For a single user installation, you can for instance place it under your `~/bin` directory, and make sure your bash profile (usually located under `~/.bash_profile`) contains the following line (or an equivalent one): `export PATH="$HOME/bin:$PATH";`

For a system-wide installation of the Accio client, you can place it under the `/usr/local/bin` directory (you need root access for that).
Make sure that accounts needing to execute Accio users have read and execution permissions on this file.
The easiest way to achieve this is to create a Unix group for Accio users and give it the permissions on the binary.
This makes sure all users of a given machine use the same version of Accio, and eases later upgrades.

## 4. Configuration
For now, you can only use a very restricted set of commands, because we have not yet configured our client how to communicate with the Accio cluster.
Configuration is done inside a `~/.accio/clusters.json` file.
You can define multiple clusters, each one having at least a name and an address where to contact it.
The first cluster ever defined is the default cluster, that is used if none is explicitly given.

An simple configuration file looks like this:

```json
[{
  "name": "default",
  "addr": "192.168.50.4:9999"
}]
```

Make sure the specified address matches the one of your actual cluster (it should be the address of a master server).
