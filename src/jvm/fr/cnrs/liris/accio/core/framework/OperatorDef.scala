package fr.cnrs.liris.accio.core.framework

import fr.cnrs.liris.common.util.Distance
import org.joda.time.Duration

import scala.reflect.runtime.universe._

sealed trait ParamType

object ParamType {

  case object Integer extends ParamType

  case object Long extends ParamType

  case object Double extends ParamType

  case object String extends ParamType

  case object StringList extends ParamType

  case object Boolean extends ParamType

  case object Duration extends ParamType

  case object Distance extends ParamType

  def of[T: TypeTag]: ParamType = of(implicitly[TypeTag[T]].tpe)

  def of(tpe: Type): ParamType = tpe match {
    case t if t =:= typeOf[Int] => Integer
    case t if t =:= typeOf[Long] => Long
    case t if t =:= typeOf[Double] => Double
    case t if t =:= typeOf[String] => String
    case t if t =:= typeOf[Boolean] => Boolean
    case t if t =:= typeOf[Duration] => Duration
    case t if t =:= typeOf[Distance] => Distance
    case t if t =:= typeOf[Seq[String]] => StringList
    case _ => throw new IllegalArgumentException(s"Invalid parameter type $tpe")
  }
}

case class ParamDef(name: String, typ: ParamType, help: Option[String], defaultValue: Option[Any], optional: Boolean)

case class OperatorDef(
    name: String,
    params: Seq[ParamDef],
    help: Option[String],
    description: Option[String],
    category: String,
    ephemeral: Boolean,
    unstable: Boolean)
