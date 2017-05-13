---
layout: docs
weight: 10
title: Installing on Ubuntu
---

This page explains how to install the Accio client on Ubuntu, starting at the 14.04 version.
Note that Accio should work on other Unix distributions.

## 1. Install requirements
The only requirement to run Accio is to have a JRE for Java 8.
Before running Accio, you need to install the Java runtime on your machine.
Please that though you only need a JRE (Java Runtime Environment) to execute Accio, you may want to install a JDK (Java Development Kit) instead;
it is perfectly fine, as the JDK includes the JRE.

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

## 2. Download binary
Download the `accio` file from [the latest release](https://github.com/privamov/accio/releases/latest).
This file is actually a simple bash wrapper around an embedded JAR.
You need to give it the executable permissions before executing it.

```bash
curl -L -o accio https://github.com/privamov/accio/releases/download/v0.6.0/accio
chmod +x accio
./accio version -client
```

## 3. Installation (optional)
### Personal installation
The file you just downloaded is directly executable by your bash, as we have seen.
However, you may want to move it under a location that is in your PATH, thus allowing you to use it from any directory.
For a single user installation, you can for instance place it under a `~/bin` directory, and make sure your bash profile (usually located under `~/.profile`) contains the following line (or an equivalent one):

```bash
export PATH="$HOME/bin:$PATH";
```

### System-wide installation
For a system-wide installation, you can place the binary under the `/usr/local/bin` directory (you need root access for that).
This ensures that all users of a given machine use the same version of Accio, and eases later upgrades.
Make sure that all accounts needing to execute Accio users have read and execution permissions on this file.

The easiest way to achieve this is to create a Unix group for Accio users and give it the permissions on the binary.
Here is an example setup, creating an `accio` Unix group:

```bash
sudo add group accio
sudo adduser $USER accio # Repeat for every user that needs Accio
sudo mv accio /usr/local/bin
sudo chown $USER:accio /usr/local/bin/accio && sudo chmod 750 /usr/local/bin/accio
```

## 4. Configuration
For now, you can only use a very restricted set of commands, because we have not yet configured our client how to communicate with the Accio cluster.
Configuration is done inside a `~/.accio/clusters.json` file (resp. `~/etc/accio/clusters.json`) for a personal installation (resp. for a system-wide installation).
You can define multiple clusters, each one having at least a name and an address where to contact it.
The first cluster ever is the default cluster, that is used if none is explicitly given.

An simple configuration file looks like this:

```json
[{
  "name": "default",
  "addr": "192.168.50.4:9999"
}]
```

Make sure the specified address matches the one of your actual cluster.
