/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.accio.core.framework

import java.util.NoSuchElementException

import fr.cnrs.liris.accio.core.param.Param
import fr.cnrs.liris.common.reflect.ReflectCaseClass

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

case class OpMeta(defn: OperatorDef, clazz: Class[Operator])

class IllegalOpDefinition(op: String, cause: Throwable)
    extends Exception(s"Illegal definition of operator $op: ${cause.getMessage}", cause)

/**
 * Operator registry stores all operators known to Accio.
 */
class OpRegistry {
  private[this] val _ops = mutable.Map.empty[String, OpMeta]

  @throws[IllegalOpDefinition]
  def register[T <: Operator : ClassTag : TypeTag]: OpMeta = {
    val clazz = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[Operator]]
    val name = clazz.getSimpleName
    require(!_ops.contains(name), s"Duplicate operator $name")

    val defn = try {
      val refl = ReflectCaseClass.of[T]
      require(refl.isAnnotated[Op], s"Operator defined in ${clazz.getName} must be annotated with @Op")
      val op = refl.annotation[Op]
      val params = refl.fields.map { field =>
        require(field.isAnnotated[Param], s"Operator parameter ${field.name} must be annotated with @Param")
        val param = field.annotation[Param]
        val optional = field.tpe <:< typeOf[Option[_]]
        val tpe = if (optional) field.tpe.baseType(typeOf[Option[_]].typeSymbol).typeArgs.head else field.tpe
        ParamDef(field.name, ParamType.of(tpe), maybe(param.help), field.defaultValue, optional)
      }
      OperatorDef(
        name = name,
        params = params,
        help = maybe(op.help),
        description = maybe(op.description),
        category = op.category,
        ephemeral = op.ephemeral,
        unstable = op.unstable)
    } catch {
      case t: Throwable =>
        throw new IllegalOpDefinition(s"Error while registering operator $name", t)
    }

    val meta = OpMeta(defn, clazz.asInstanceOf[Class[Operator]])
    _ops(name) = meta
    meta
  }

  def ops: Seq[OpMeta] = _ops.values.toSeq.sortBy(_.defn.name)

  /**
   * Check whether the registry contains an [[Operator]] for the given name.
   *
   * @param name An operation name
   * @return True if there is an operation for the given name, false otherwise
   */
  def contains(name: String): Boolean = _ops.contains(name)

  /**
   * Return the [[Operator]] class for the given name, if it exists.
   *
   * @param name An operation name
   */
  def get(name: String): Option[OpMeta] = _ops.get(name)

  /**
   * Return the [[Operator]] class for the given name.
   *
   * @param name An operation name
   * @throws NoSuchElementException If there is no operation for the given name
   */
  @throws[NoSuchElementException]
  def apply(name: String): OpMeta = _ops(name)

  private def maybe(str: String) = if (str.isEmpty) None else Some(str)
}