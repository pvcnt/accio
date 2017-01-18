---
layout: docs
nav: docs
title: Location privacy
---

Although the Accio framework is agnostic to the underlying operators, the way they are implemented and data they manipulate, Accio has been historically built to study location privacy.
It is visible when we look at the data types supported by the [Accio data model](workflow-dsl.html), which contains several spatio-temporal-related types.
Moreover, Accio is for now only packaged with a operators aiming at studying location privacy.

The standard set of operators encompasses many state-of-the-art algorithms about location privacy, such as Location Privacy Protection Mechanisms (LPPMs), evaluation metrics, as well as generic spatio-temporal data manipulation tools.
Accio has been used and is being used by privacy researchers to help them launch experiments and analyze their results.

In this section of the documentation, you will find more details about [the abstractions we use](data-model.html) around location privacy and [the library of operators](operators-library.html) we defined.