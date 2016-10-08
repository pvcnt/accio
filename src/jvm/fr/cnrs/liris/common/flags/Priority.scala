// Large portions of code are copied from Google's Bazel.
/*
 * Copyright 2014 The Bazel Authors. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding copyright ownership.  The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License
 * for the specific language governing permissions and limitations under the License.
 */

package fr.cnrs.liris.common.flags

/**
 * The priority of flags values.
 *
 * In general, new values for flags can only override values with a lower or equal priority.
 * Flag values provided as default value in a flags class are implicitly at the
 * priority [[Priority.Default]].
 */

sealed class Priority(protected val level: Int) extends Ordered[Priority] {
  override def compare(that: Priority): Int = level.compareTo(that.level)
}

object Priority {

  /**
   * The priority of values specified in the field default value. This should never be specified
   * in calls to [[FlagsParser.parse]].
   */
  case object Default extends Priority(0)

  /**
   * Overrides default options at runtime, while still allowing the values to be
   * overridden manually.
   */
  case object ComputedDefault extends Priority(1)

  /**
   * For options coming from a configuration file or rc file.
   */
  case object RcFile extends Priority(2)

  /**
   * For options coming from the command line.
   */
  case object CommandLine extends Priority(3)

  /**
   * For options coming from invocation policy.
   */
  case object InvocationPolicy extends Priority(4)

  /**
   * This priority can be used to unconditionally override any user-provided options.
   * This should be used rarely and with caution!
   */
  case object SoftwareRequirement extends Priority(5)

}
