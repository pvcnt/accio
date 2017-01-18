---
layout: accio
nav: accio
section: developpers
title: Documenting operators
---

Documentation is essential to let other people use your operators easily.
Documentation can be added on operator level and port level, to describe how they work.

* TOC
{:toc}

## In-code documentation

As introduced in the page about [implementing a custom operator](operator.html), the first step is to annotation operators and ports with appropriate annotations.
In addition of providing essential information for the Accio framework, these annotations also allow to document your operators directly in the code.

### Annotating operators

The `@Op` annotation is used to annotation operators.
The following arguments, all optional, can be used for documentation purposes.

  * `help` is used to provide a short (i.e., one-line) help message.
  It is usually displayed when listing operators to provide a quick summary of what it does.
  * `description` is used to provide a longer description.
  It is a good place to describe in more details the operator's behavior and its inner-working.
  It is usually displayed after the short help message.
  You can either write the description in-line in a string or load an external Java resource, located in the same directory than your operator, and reference this file by using as a description `resource:name_of_your_file.txt`.
  * `category` is used to categorize operators.
  By default, all operators fall into the "misc" category.
  * `deprecation` is used to indicate that an operator is deprecated.
  If you fill this argument, a warning will appear in documentation and when the operator is used, with the deprecation message.


### Annotating ports

The `@Arg` annotation is used to annotate ports.
It has a single `help` argument that can be used to describe the usage of any input or output port.
All other metadata is automatically extracted from the case class definition.

## Generating documentation

Accio comes with a tool to automatically generate a documentation page for registered operators.
This tool is under the `fr.cnrs.liris.accio.docgen` package, and can be built as usual using Pants.
You can build and run it in two simple steps:

```bash
$ ./pants binary src/jvm/fr/cnrs/liris/accio/docgen:bin
$ java -jar dist/accio-docgen.jar -out=docs/privacy/library.md
```

It generates documentation for all operators registered in the `fr.cnrs.liris.privamov.ops.OpsModule` Guice module.
