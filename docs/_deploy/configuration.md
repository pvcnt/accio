---
layout: deploy
title: Configuration
---

The Accio agent accepts a large number of configuration options, which are specified through command-line arguments.
As such, all the options are specified either directly in the command-line, or inside a startup script (such as a Shell script, or a systemd configuration file).

A typical command to start the agent looks like:
```bash
java $JAVA_OPTS -jar /opt/accio-gateway.jar $ACCIO_OPTS
```
where `JAVA_OPTS` contains the flags used to tune the JVM and `ACCIO_OPTS` contains the Accio-specific flags.

* TOC
{:toc}

## JVM configuration

First of all, because Accio runs on the JVM, it is particularly important to configure it correctly.
The exact settings are dependent on your machine and the expected workload.
As a starting point, we recommend:

* The initial (`-Xms`) and maximum (`-Xmx`) heap size should be identical to prevent heap resizing.
* Either `-XX:+UseConcMarkSweepGC` *or* `-XX:+UseG1GC -XX:+UseStringDeduplication` are sane defaults for the garbage collector.
* `-Djava.net.preferIPv4Stack=true` makes sense in most cases.

Tuning the JVM is a rather complex topic, and a science in itself.
The interested reader will find a [lot](https://javaworks.wordpress.com/2013/06/25/jvm-parameters-you-should-know/) [of](http://blog.sokolenko.me/2014/11/javavm-options-production.html) [materials](https://www.slideshare.net/aszegedi/everything-i-ever-learned-about-jvm-performance-tuning-twitter/58-Cassandra_slab_allocator_2MB_slab) on the Web.

## Network configuration

By default, the agent binds itself to all interfaces and listens on the 9999 port, while the administrative interface listens on the 9990 port.
You may explicitly specify those ports and interfaces with the `-thrift.port` and `-admin.port` flags.
They take as argument a hostname and a port number, delimited by a colon.
The hostname may be left empty to bind on all interfaces.

For example, to bind the admin interface on localhost, and make the agent listen on port 8080 on all interfaces, you would use the following flags:
```
-thrift.port=:8080
-admin.port=127.0.0.1:9990
```

## Storage configuration

The runtime information about workflows and runs is persisted inside a storage.
Accio comes with several implementations for the storage, among which only one can be active.
The storage type is configured with the `-storage` flag.
By default, an in-memory storage is used, which means that the data is lost once the server stops.
You may certainly want to switch to a more durable alternative, such as a MySQL database.
The description of all available storages and their respective configuration is available on [the dedicated page](storage.html).

## Scheduler configuration

The scheduler is in charge of launching and monitoring the execution of tasks contained inside runs.
The tasks are launched via the executor, which is a small executable in charge of parsing the information about the task to execute, instantiate the corresponding operator, execute it and write the result into a file.

The executor indeed obeys to a simple contract: it receives two command-line arguments:
* the first one is an encoded representation of the task to execute.
* the second one is the path to a file where to write the result.

The task to execute is represented as the `Task` Thrift structure, which is then encoded by the Thrift binary protocol and finally base64-encoded and received as the first argument.
The result is represented as the `OpResult` Thrift structure, which is then encoded by the Thrift binary protocol and stored in the output file specified as the second argument.
The Thrift structures manipulated by Accio are defined in the [`api.thrift` file](https://github.com/privamov/accio/blob/master/accio/thrift/fr/cnrs/liris/accio/api/api.thrift).

The location of the executor is specified by the `-executor_uri` flag.
Normally, it should point to the official Accio executor, e.g.:
```
-executor_uri=/opt/accio-executor.jar
```

Accio comes with several implementation for the scheduler, among which only one can be active.
The scheduler type is configured with the `-scheduler` flag.
The description of all available storages and their respective configuration is available on [the dedicated page](scheduler.html).
