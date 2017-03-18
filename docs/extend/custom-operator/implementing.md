---
layout: docs
weight: 10
title: Implementing an operator
---

Operators are the basic building block of workflows.
Accio comes with some built-in operators, but you may need to implement new ones.
Opeators need to be implemented directly in [Scala](http://www.scala-lang.org), though this may evolve in the future.

* TOC
{:toc}

## 1. Create a new operator class
As of now, all operators are implemented inside the `src/jvm/fr/cnrs/liris/privamov/ops` module.
You may decide to create a new module for a new operator or new family of operators, or stick inside this module.
The first step is to decide a name for your operator and create a new class for its implementation.
By convention, all operators' names finish with the "Op" suffix (that is later automatically removed from its actual name).

You may need to add some dependencies to the `BUILD` file if you wish to add new libraries.
The following Accio-related dependencies must be included in any module containing operator implementations:

  * `src/jvm/fr/cnrs/liris/accio/core/api` contains interfaces and standard data types
  * `src/jvm/fr/cnrs/liris/accio/core/api:annotations` contains Java annotations.

## 2. Implement the operator class
Then, you will need to implement the `Operator[In,Out]` interface and its `execute` method.

```scala
import fr.cnrs.liris.accio.core.api._

@Op(
  category = "useless",
  help = "An operator that does absolutely nothing")
class TotallyUselessOp extends Operator[Unit, Unit] {
  override def execute(in: Unit, ctx: OpContext): Unit = {
    println("My operator is running and doing nothing")
  }
}
```

This is an operator that simply prints something to the standard output but does actually nothing.
A few things are worth noting so far.
  * Operators must be annotated with an `@Op` annotation.
Whereas all arguments are optional, it is very useful to provide information to the end-user about what this operator does.
By default, the operator name is the simple name of the class (i.e., without the namespace), with the "Op" suffix stripped.
You may override this by explicitly specifying a `name` argument.
  * This operator has no input and no output, indicated with the `Unit` type argument (first is for input, second for output).

**The execution of any operator must be deterministic and reproducible.**
In other words, given some inputs, it should always produce the exact same outputs.
It means the implementation must not use any source of external randomness such as generating random numbers or using current system time.
Moreover, operators should be stateless.
They are always executed on a single machine, this is the basic execution unit.
They provide some resource constraints on the number of CPUs, the quantity of RAM and disk space they need to execute properly;
they are guaranteed to be allocated at least these resources and must ensure not to overflow them.

## 3. Define the inputs and outputs
Because an operator without any input or output is not exactly very useful, we will see how to define that.
This is a contract for the operator, whose implementation is basically just responsible for converting inputs (received as first argument of the `execute` method) into outputs (the return value of the `execute` method).
This means that an operator implementation can be used very easily outside of the context of Accio.

Inputs and outputs are defined with Scala case classes, each constructor argument becoming an input/output argument for the operator.
By convention the input class is named after the operator suffixed with "In" (instead of "Op") and the output class is named after the operator suffixed with "Out" (instead of "Op").

```scala
import fr.cnrs.liris.accio.core.api._

@Op(
  category = "numeric",
  help = "An operator that multiplies a number by another one")
class MultiplyOp extends Operator[MultiplyIn, MultiplyOut] {
  override def execute(in: MultiplyIn, ctx: OpContext): MultiplyOut = {
    MultiplyOut(in.a * in.b)
  }
}

case class MultiplyIn(@Arg a: Int, @Arg b: Int)

case class MultiplyOut(@Arg c: Int)
```

This operator has two integer input ports, `a` and `b` that will get multiplied together to fill a single integer port, `c`.
A bunch of new things are worth noting.

  * Input and output arguments must be annotated with an `@Arg` annotation.
  * All arguments of the input class must be mapped to ports, whereas some arguments of the output class may be left apart.
  In this case you will not annotate them with `@Arg` and they will be ignored by Accio.
  * Input and output arguments must be of one of the authorized data types in Accio.
  It is enforced at runtime.
  Additionally, input arguments may be of an optional type of a valid data type.
  This allows the operator to executed even if no value is specified for this port.
  Output arguments must always be specified.

Operator and port annotations come with various options useful to document them.
It can even be used to automatically generate a documentation ready to be integrated on a web site.
You can learn more about this topic on the [dedicated page](documenting-operators.html).

## 4. Implement your operator
Most of the work for a developper is to implement the body of the `execute` method.
You have access to a context, which gives you access to the following methods:

  * `workDir`: Path to a directory where you can safely write data.
  This directory is a temporary directory, that will be deleted once the operator is completed.
  * `env`: A [Sparkle](sparkle.html) environment, allowing you to process large amounts of data efficiently.
  * `seed`: A long, to be used with [unstable operators](#working-with-randomness).
  * `read` and `write`: Helpers methods to read and write datasets in the working directory, in conjunction with Sparkle.

You do **not** have to care about transferring artifacts in this method.
Indeed, some artifacts are references to some binary content (e.g., datasets).
Such artifacts will be automatically downloaded for you before an operator starts and be made accessible in the working directory.
Conversely, these artifacts will be automatically uploaded once an operator finishes, before the working directory is deleted.

## 5. Register your operator
All operators are registered in the `fr.cnrs.liris.privamov.ops.OpsModule` class, through the [Guice](https://github.com/google/guice) framework.
Operators are registered by their class name (not an instance of them).
You simply need to add a new line to register your operator.

```scala
import com.google.inject.TypeLiteral
import fr.cnrs.liris.accio.core.api.Operator
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}

object OpsModule extends ScalaModule {
  override def configure(): Unit = {
    val ops = ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Class[_ <: Operator[_, _]]] {})
    // Many other operators registrations.
    // Here goes our new operator.
    ops.addBinding.toInstance(classOf[MultiplyOp])
  }
}
```
