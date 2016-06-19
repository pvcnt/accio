Accio is both a location privacy framework and a tool.
It provides a framework allowing to develop mobility traces manipulation operations, compose them and run them on datasets.
It also provides a tool to describe complex workflows, run them and analyze the results.

Compiling Accio
---------------
To compile Accio you will need Python 2.x plus development headers (in order to use the Pants build tool) and Java 8.
You do not need to have Scala installed if you just want to compile the code.
Accio can be compiled as a JAR in one command:

```bash
./pants binary src/jvm/fr/cnrs/liris/accio/cli:bin
```

An `accio-bin.jar` will then appear in the `dist/` folder.

If you have [Vagrant](https://www.vagrantup.com/) and [VirtualBox](https://www.virtualbox.org/) installed on your machine, you can otherwise launch a development environment via `vagrant up`.
The source code will be located under `/vagrant`, with tools to compile and launch it already installed.