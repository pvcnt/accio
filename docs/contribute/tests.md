---
layout: contribute
title: Running tests
---

We aim at having a decent code coverage for Accio, allowing us to be confident that everything behaves well, and moving with the confidence of avoiding regressions.
This page explains how to run the test suit.

## Running unit tests

The following command is used to run all unit tests.
```bash
bazel test ...
```

Please not that is implicitly builds all targets, independently on whether there are actually tests, and then run all targets that correspond to tests.
It means that all committed code must at least compile, even if not actually used by the system.

### MySQL service

In order to test the storage, a local MySQL database must be available.
By default, the unit tests look for a MySQL server listening on `localhost:3306`, with a passwordless user `root`.
It is possible to override the MySQL hostname, user or password with, respectively, environment variables `MYSQL_HOST`, `MYSQL_USER` and `MYSQL_PASSWORD`.
A temporary database will be created for each test requiring it; such databases will follow the naming scheme `test_<random suffix>`.
It means that the given user should have the right to create databases.
If you cannot setup a local MySQL database, you can also ignore entirely the tests requiring the latter by setting the environment variable `MYSQL_DISABLED=yes`.

Please note that Bazel works on an isolated environment, which means that you must use the `--action_env` flag to make some custom environment variables available to Bazel tests.
For example, if you want to override the MySQL credentials, you may use the following command:
```bash
bazel test --action_env MYSQL_USER=accio --action_env MYSQL_PASSWORD=secret ...
```

## Continuous integration

We use [Travis CI](https://travis-ci.org/privamov/accio) to provide continuous integration.
The test suit is launched automatically after code has been pushed and for pull requests.
