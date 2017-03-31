---
layout: docs
weight: 30
title: Installing on Windows
---

This page explains how to install the Accio client on Windows.

## 1. Install requirements
The only requirement to run Accio is to have a JRE for Java 8.
Before running Accio, you need to install the Java runtime on your machine.
Please that though you only need a JRE (Java Runtime Environment) to execute Accio, you may want to install a JDK (Java Development Kit) instead;
it is perfectly fine, as the JDK includes the JRE.

Java 8 JRE can be downloaded from [Oracle's JRE Page](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html).
Look for "Windows x64 Offline" under "Java SE Runtime Environment".
This will download an executable file with an install wizard.

## 2. Download JAR
Download the `accio.jar` file from [the latest release](https://github.com/privamov/accio/releases/latest).
This file can be executed through the JRE previously installed.
Because this is a command-line application, you need to use a shell to execute it.

```bash
java -jar accio.jar version -client
```

## 3. Configuration
For now, you can only use a very restricted set of commands, because we have not yet configured our client how to communicate with the Accio cluster.
Configuration is done inside a `.accio/clusters.json` file in your home directory.
You can define multiple clusters, each one having at least a name and an address where to contact it.
The first cluster ever defined is the default cluster, that is used if none is explicitly given.

An simple configuration file looks like this:

```json
[{
  "name": "default",
  "addr": "192.168.50.4:9999"
}]
```

Make sure the specified address matches the one of your actual cluster.
