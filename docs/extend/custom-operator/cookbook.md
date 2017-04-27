---
layout: docs
weight: 40
title: Cookbook
---

We provide here some recipes for more advanced use cases.

* TOC
{:toc}

### Using external dependencies

Your operators may have dependencies to other objects.
For that purpose, we provide integration with the [Guice](https://github.com/google/guice) dependency injection framework.
All operators are created using a Guice injector, which mean you can use usual `@Inject` (and other) annotations to have your dependencies automatically injected.
You may also need to modify the `OpsModule` Guice module to wire any new interface to its implementation.

### Working with randomness

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
import fr.cnrs.liris.accio.framework.sdk._
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
