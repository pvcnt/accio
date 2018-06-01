Lumos
=====

Lumos is a tool to persist the state of jobs.
It is specially intended for data-intensive jobs, which may be broken up into one or several tasks.
It follows an event-based model, where events are pushed to create new jobs or alter existing ones.
It comes with a server, a gateway, and a CLI application.

  * The server is in charge of processing events and gives access to jobs and data.
  It is a Thrift service.
  * The gateway provides a REST API on top of the server.
  It also comes with a Web interface allowing to search for and visualize jobs.
  * The CLI application is a Thrift client to the server.

Lumos is completely independent from Accio.