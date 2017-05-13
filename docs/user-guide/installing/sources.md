---
layout: docs
weight: 40
title: Installing from sources
---

This guide explains how to compile the Accio client from the source code.
If possible, prefer using a pre-compiled binary for Linux, Mac OS or Windows.
Building from source is only possible on Linux and Mac OS, as our build tool is not supported on Windows.

## 1. Clone the Git repository
The source code is hosted inside a Git repository  [on GitHub](https://github.com/privamov/accio).
You need to [install Git](https://git-scm.com/downloads) first, if it is not already done.
You can directly clone the repository to get the latest version of the source code.

```bash
git clone git@github.com:privamov/accio.git
cd accio
```

We recommend avoiding cloning the repository in a folder whose path contains special characters such as accents.
It has already been shown to cause Pants to fail.
All the development is done on the `master` branch, and releases are tagged.
You can hence build a specific version:

```bash
git checkout v0.6.0 # Replace the version number with the one you target
```

## 2. Install build dependencies
The following dependencies must be met in order to build Accio:

  * [Java JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) is required to run both Pants and Accio.
  * [Python 2.7.x](https://www.python.org/) and its development headers are required by Pants (*not* Python 3.x).
  * [gcc/G++](https://gcc.gnu.org/) is required by Pants.
  * Internet access is required to download dependencies both for Pants and Accio.

[Pants](http://www.pantsbuild.org/) is our build tool of choice.
It is bootstrapped by using the `pants` wrapper present at the root of the repository.
You can validate your build environment by checking Pants version.
It may take some time the first you run this command, as Pants and its dependencies will be downloaded.

```bash
./pants -V
```

If this step fails, you may want to refer to [Pants installation instructions](http://www.pantsbuild.org/install.html).

## 3. Build the client
You can now build Accio client from source.
While it is possible to use directly `./pants binary`, there is a wrapper script handling some other operations, such as creation a bash wrapper around the generated JAR file.
You can invoke it by running the following command from the root of the repository:

```bash
./bin/acciobuild.sh client
```

This will create two files under the `dist/` directory: `accio.jar` and `accio`.
The former is a regular cross-platform JAR file.
It can be executed wherever a Java JRE is available, typically using the following command:

```bash
java -jar dist/accio.jar version -client
```

The latter is a bash wrapper around this JAR file, allowing to launch it directory by typing its name, without specifying the Java executable and options.
This wrapper already embeds the JAR file (which means there is no need for the `accio.jar` file to be next to the `accio` script).

```bash
./dist/accio version -client
```

<div class="alert alert-info" markdown="1">
  :mag: The extend section contains full instructions [how to compile and develop Accio](../../extend/compiling/).
</div>
