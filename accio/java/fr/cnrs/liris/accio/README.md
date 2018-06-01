Accio
=====

Accio is the workflow execution platform.
It comes with a server, an executor, an SDK, and a CLI application.

  * The server is in charge of accepting workflows, scheduling and monitoring them.
  It is a Thrift service.
  * The executor is a binary responsible for coordinating the execution of an entire workflow.
  It spawns individual operators as sub-processes.
  * The SDK provides a programming interface for implementing new operators.
  Operators rely on a simple interface based on Thrift, although we provide a Scala SDK to ease the implementation.
  * The CLI application is a Thrift client to the server.

Accio in itself is stateless, and as such it depends on Lumos to persist the state of jobs.