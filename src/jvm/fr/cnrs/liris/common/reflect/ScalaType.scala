// Copied from Twitter's Finatra.
/*
 * Copyright (C) 2016 Twitter
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

package fr.cnrs.liris.common.reflect

import scala.tools.scalap.scalax.rules.scalasig.TypeRefType

case class ScalaType(typeRefType: TypeRefType) {
  private[this] val path = typeRefType.symbol.path

  val runtimeClass: Class[_] = CaseClassSigParser.loadClass(path)

  def primitiveAwareErasure: Class[_] = primitiveAwareLoadClass(path)

  val typeArguments: Seq[ScalaType] = {
    val typeArgs = typeRefType.typeArgs.asInstanceOf[Seq[TypeRefType]]
    typeArgs.map(ScalaType.apply)
  }

  def isPrimitive: Boolean = runtimeClass != classOf[AnyRef]

  def isCollection: Boolean = classOf[Iterable[Any]].isAssignableFrom(runtimeClass)

  def isMap: Boolean = classOf[Map[Any, Any]].isAssignableFrom(runtimeClass)

  def isArray: Boolean = path == "scala.Array"

  def isUnit: Boolean = path == "scala.Unit"

  def isEnum: Boolean = runtimeClass.isEnum

  override def toString: String =
    runtimeClass.getName + (if (typeArguments.nonEmpty) "[" + typeArguments.map(_.toString).mkString(",") + "]" else "")

  /* Needed to support Array creation (Derived from Jerkson) */
  private def primitiveAwareLoadClass(path: String): Class[_] = path match {
    case "scala.Boolean" => classOf[Boolean]
    case "scala.Byte" => classOf[Byte]
    case "scala.Char" => classOf[Char]
    case "scala.Double" => classOf[Double]
    case "scala.Float" => classOf[Float]
    case "scala.Int" => classOf[Int]
    case "scala.Long" => classOf[Long]
    case "scala.Short" => classOf[Short]
    case _ => CaseClassSigParser.loadClass(path)
  }
}
