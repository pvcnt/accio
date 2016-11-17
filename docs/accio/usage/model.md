---
layout: accio
nav: accio
title: Accio data model
---

The following scalar data types are supported for Accio input and output ports:

  * byte
  * short
  * integer
  * long
  * double
  * boolean
  * location
  * distance
  * timestamp
  * duration

The following complex data type is supported for Accio input and output ports:
  * dataset

The following composed data types are supported for Accio input and output ports:

  * set of a scalar data type
  * list of a scalar data type
  * map of scalar data types as keys and values

Each data type is mapped into the appropriate Scala type.