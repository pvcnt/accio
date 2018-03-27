---
layout: contribute
title: Creating a custom operator
---

This page provides a guide to implement a custom operator, step by step.
More advanced topics are covered in other pages of this section.

* TOC
{:toc}

## 1. Creating a new operator class
As of now, all operators are implemented inside the `src/jvm/fr/cnrs/liris/locapriv` module.
You may decide to create a new module for a new operator or new family of operators, or stick inside this module.
The first step is to decide a name for your operator and create a new class for its implementation.
By convention, all operators' names finish with the "Op" suffix (that is later automatically removed from its actual name).

You may need to add some dependencies to the `BUILD` file if you wish to add new libraries.
The following Accio-related dependencies must be included in any module containing operator implementations:

  * `src/jvm/fr/cnrs/liris/accio/sdk` contains interfaces and standard data types
  * `src/jvm/fr/cnrs/liris/accio/sdk:annotations` contains Java annotations.

Then, you will need to create a class that extends `Operator[In,Out]` trait.

```scala
import fr.cnrs.liris.accio.sdk._

class MultiplyOp extends Operator[Unit, Unit] {
  override def execute(in: Unit, ctx: OpContext): Unit = ???
}
```

## 2. Defining the interface
Because an operator without any input or output is not exactly very useful, we will see how to define that.
This is a contract for the operator, whose implementation is basically just responsible for converting inputs (received as first argument of the `execute` method) into outputs (the return value of the `execute` method).
This means that an operator implementation can be used very easily outside of the context of Accio.

Inputs and outputs are defined with Scala case classes, each constructor argument becoming an input/output argument for the operator.
By convention the input class is named after the operator suffixed with "In" (instead of "Op") and the output class is named after the operator suffixed with "Out" (instead of "Op").

```scala
import fr.cnrs.liris.accio.sdk._

@Op(
  category = "numeric",
  help = "An operator that multiplies a number by another one")
class MultiplyOp extends Operator[MultiplyIn, MultiplyOut] {
  override def execute(in: MultiplyIn, ctx: OpContext): MultiplyOut = ???
}

case class MultiplyIn(@Arg a: Int, @Arg b: Int)

case class MultiplyOut(@Arg c: Int)
```

The `@Op` annotation is mandatory for an operator to be recognised as such (implementing the `Operator` interface is not enough).
This annotation provides essential metadata are used by Accio.
Whereas all its fields are optional, you may want to override some of them, e.g., to provide information to the end-user about what this operator does in the `help` field.
By default, the operator name is the simple name of the class (i.e., without the namespace), with the "Op" suffix stripped.
You may override this by explicitly specifying a `name` field.

Among other things, computational resources with the `cpu`, `ram` and `disk` fields can also be requested via the `@Op` annotation.
Each operator is executed in a sandbox and need to declare resources it needs.
Defaults values are defined but may not be convenient in all cases.

This operator has two integer input ports, `a` and `b` that will get multiplied together to fill a single integer port, `c`.
A bunch of new things are worth noting.

  * Input and output arguments must be annotated with an `@Arg` annotation.
  * All arguments of the input class must be mapped to ports, whereas some arguments of the output class may be left apart.
  In this case you will not annotate them with `@Arg` and they will be ignored by Accio.
  * Input and output arguments must be of one of the authorised data types in Accio.
  It is enforced at runtime.
  Additionally, input arguments may be of an optional type of a valid data type.
  This allows the operator to executed even if no value is specified for this port.
  Output arguments must always be specified.

In addition to providing essential information to Accio, these annotations are also used to automatically generate some documentation.

## 3. Implementing the operator
Most of the work for a developer is to implement the body of the `execute` method.

```scala
import fr.cnrs.liris.accio.sdk._

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

You have access to an execution context, which gives you access to the following methods:

  * `workDir`: Path to a directory where you can safely write data.
  This directory is a temporary directory, that will be deleted once the operator is completed.
  * `seed`: A "random" 64 bits integer, can can be used in [unstable operators](#working-with-randomness).

You have to be careful to stay within requested resources.
You are guaranteed to have the number of CPU cores and the amount of RAM and storage you requested; you may have more, but maybe not.
It is the developer's job to guarantee that memory will not be exceeded and the content written will not overflow the disk.
It can be tempting to request more than actually needed; however, the whole point of Accio is allow to run experiments in parallel on a cluster, which is not possible if all operators consume the entire resources of a machine while they do not need them.

**The execution of any operator must be deterministic and reproducible.**
In other words, given some inputs, it should always produce the exact same outputs.
It means the implementation must not use any source of external randomness such as generating random numbers or using current system time.
Moreover, operators should be stateless.
They are always executed on a single machine, this is the basic execution unit.
They provide some resource constraints on the number of CPUs, the quantity of RAM and disk space they need to execute properly;
they are guaranteed to be allocated at least these resources and must ensure not to overflow them.

## 4. Registering the operator
All operators are automatically discovered as long as they implement the `Operator` interface and they are in the JVM classpath.
You have nothing else to do to register it to Accio.

## Working with randomness

It was said previously that the execution of operators must be perfectly deterministic.
But some operators need to perform random operators, such as sampling or adding noise.
Of course this is possible with Accio, if you respect some simple rules.

First, you need to declare your operator as *unstable*.
It means that its implementation perform random operations.
To do that, you must use the `unstable` flag of the `@Op` annotation.
It means that an operator is statically declared as unstable or not, it cannot depend on its inputs.

The **only** source of randomness that you can use now is the one provided by the `OpContext.seed` field.
This field contains a long that can be used in `scala.util.Random` and other sources of randomness as a seed.
This seed has been initialized either randomly, or from the master seed that has been specified when launching the experiment.
It allows to reproduce experiments by using the exact same source of randomness.
Other sources of randomness, such as system time, should still be banished for the sake of results' reproducibility.
Operators not declared as unstable will not have access to this seed and an exception will be raised.

```scala
import fr.cnrs.liris.accio.sdk._
import scala.util.Random

@Op(
  category = "numeric",
  help = "An operator that multiplies a number by another one and optionally adds a number")
class MultiplyOp extends Operator[MultiplyIn, MultiplyOut] {
  override def execute(in: MultiplyIn, ctx: OpContext): MultiplyOut = {
    val plus = if (in.add) {
      val rnd = new Random(ctx.seed)
      rnd.nextDouble()
    } else 0d
    MultiplyOut(in.a * in.b + plus)
  }

  override def isUnstable(in: MultiplyIn): Boolean = in.add  
}

case class MultiplyIn(@Arg a: Int, @Arg b: Int, @Arg add: Boolean)

case class MultiplyOut(@Arg c: Int)
```

## Documenting operators

Operators and ports are annotated with annotations that provide runtime information.
In addition of providing essential information for the Accio framework, they also allow to document the operators directly in the code.

The `@Op` annotation is used to annotate operators.
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


The `@Arg` annotation is used to annotate ports.
It has a single `help` argument that can be used to describe the usage of any input or output port.
All other metadata is automatically extracted from the case class definition.
