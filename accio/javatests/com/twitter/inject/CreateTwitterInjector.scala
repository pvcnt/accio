/*
 * Copyright 2017-2018 UCL / Vincent Primault <v.primault@ucl.ac.uk>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.inject

import com.google.inject.{Guice, Module}
import com.twitter.app.{Flag, Flags}

trait CreateTwitterInjector {
  protected def modules: Seq[Module]

  protected def allowUndefinedFlags: Boolean = false

  protected def createInjector: Injector = createInjector(Array.empty[String])

  protected final def createInjector(args: String*): Injector = createInjector(args.toArray)

  protected final def createInjector(args: Array[String]): Injector = {
    val allModules = modules.flatMap(collectModules)
    val flags = new Flags("testing", includeGlobal = false)
    modules.flatMap(collectFlags).foreach(flags.add)
    flags.parseOrExit1(args, allowUndefinedFlags)
    Injector(Guice.createInjector(allModules: _*))
  }

  private def collectModules(module: Module): Seq[Module] = {
    module match {
      case m: TwitterBaseModule => module +: m.modules.flatMap(collectModules)
      case _ => Seq(module)
    }
  }

  private def collectFlags(module: Module): Seq[Flag[_]] = {
    module match {
      case m: TwitterBaseModule => m.flags ++ m.modules.flatMap(collectFlags)
      case _ => Seq.empty
    }
  }
}
