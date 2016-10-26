---
layout: documentation
nav: docs
title: Compiling Accio
---

Accio has been successfully compiled on Mac OS X (10.10, 10.11 and 10.12) and Ubuntu (14.04 and 16.04).
This page describes how to successfully get started to develop Accio.

Accio can be built either entirely or component by component.
It is also possible to [run the test suit](testing.html).

* TOC
{:toc}

## Requirements

The following dependencies must be met in order to compile Accio:

  * [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) is required to run both Pants and Accio.
  * [Python 2.7.x](https://www.python.org/) and its development headers are required by Pants (*not* Python 3.x).
  * [gcc/G++](https://gcc.gnu.org/) is required by Pants.
  * [Git](https://git-scm.com/) is needed to keep Accio's source code up-to-date.
  * Internet access is required to download dependencies both for Pants and Accio.
  
Alternatively, if you have [Vagrant](https://www.vagrantup.com) and [VirtualBox](https://www.virtualbox.org) installed on your machine, you can launch a fully configured development environment via `vagrant up`.
The source code will be located under the `/vagrant` folder, with tools to compile and launch it already installed.

## Getting the source code

The source code is hosted on [a private repository on GitHub](https://github.com/pvcnt/location-privacy).
You can directly clone the repository to get the latest version of the source code.

```bash
$ git clone git@github.com:pvcnt/location-privacy.git
```

Please avoid cloning the repository in a local folder whose path contains special characters such as accents.
It may cause Pants to fail.
All the development is done on the `master` branch, and releases are tagged.

## Installing Pants

Accio is compiled using the [Pants](http://pantsbuild.org) build tool.
Pants is usually not installed globally but rather once per repository.
If all requirements are fulfilled, you should be able to bootstrap Pants automatically by calling it from the sources root.

```bash
$ cd /path/to/accio
$ ./pants -V
1.1.0
```

In most settings this command should be sufficient.
If you need more help about Pants, you can read the appropriate [installation instructions](http://www.pantsbuild.org/install.html).

## Building Accio

Pants is used to produce executable JAR files for Accio, which will appear in the `dist/` folder.
You can build the main executable using the following command.

```bash
$ ./pants binary src/jvm/fr/cnrs/liris/accio/cli:bin
```

If the compilation is successful, an `accio.jar` JAR will appear in the `dist/` folder.

## Running Accio

When developing, you can also run Accio directly, which involves recompiling changed files on-the-fly before.

```bash
$ ./pants run src/jvm/fr/cnrs/liris/accio/cli:bin -- command line arguments to accio
```