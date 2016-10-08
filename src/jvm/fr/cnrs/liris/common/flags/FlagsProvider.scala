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

import scala.reflect.ClassTag

/**
 * A read-only interface for flags parser results, which only allows to query the flags of a
 * specific class, but not e.g. the residue any other information pertaining to the command line.
 */
trait FlagsClassProvider {
  /**
   * Return the flags instance for the given class parameter, that is, the parsed flags, or null if
   * it is not among those available.
   */
  def as[T: ClassTag]: T
}

/**
 * A read-only interface for flags parser results, which does not allow any further parsing
 * of flags.
 */
trait FlagsProvider extends FlagsClassProvider {
  /**
   * Return the residue, that is, the arguments that have not been parsed.
   */
  def residue: Seq[String]

  /**
   * Return if the named flag was specified explicitly in a call to parse.
   */
  def containsExplicitFlag(string: String): Boolean

  /**
   * Return the list of all flags that were specified either explicitly or implicitly. These flags
   * are sorted by priority, and by the order in which they were specified. Does not include
   * the residue.
   */
  def asListOfUnparsedFlags: Seq[UnparsedFlagValueDescription]

  /**
   * Return a list of all explicitly specified flags, suitable for logging or for displaying back
   * to the user. These flags are sorted by priority, and by the order in which they were
   * specified. Does not include the residue.
   *
   * The list includes undocumented flags.
   */
  def asListOfExplicitFlags: Seq[UnparsedFlagValueDescription]

  /**
   * Return a list of all flags, including undocumented ones, and their effective values. There is
   * no guaranteed ordering for the result.
   */
  def asListOfEffectiveFlags: Seq[FlagValueDescription]

  /**
   * Canonicalize the list of flags that this [[FlagsParser]] has parsed. The contract is that if
   * the returned set of flags is passed to an flags parser with the same flags classes, then that
   * will have the same effect as using the original args (which are passed in here), except for
   * cosmetic differences.
   */
  def canonicalize: Seq[String]
}
