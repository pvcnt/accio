package fr.cnrs.liris.accio.core.framework

import java.util.NoSuchElementException

import fr.cnrs.liris.accio.core.param.Param
import fr.cnrs.liris.common.reflect.ReflectCaseClass

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

case class OpMeta(defn: OperatorDef, clazz: Class[Operator])

class IllegalOpDefinition(clazz: Class[_], cause: Throwable)
    extends Exception(s"Illegal definition of operator ${clazz.getName}: ${cause.getMessage}", cause)

trait OpMetaReader {
  def read[T <: Operator : ClassTag : TypeTag]: OpMeta
}

class AnnotationOpMetaReader extends OpMetaReader {
  override def read[T <: Operator : ClassTag : TypeTag]: OpMeta = {
    val clazz = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[Operator]]
    val defn = try {
      val refl = ReflectCaseClass.of[T]
      require(refl.isAnnotated[Op], s"Operator must be annotated with @Op")
      val op = refl.annotation[Op]
      val name = if (op.name.nonEmpty) op.name else clazz.getSimpleName.stripPrefix("Op")
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
        inputs = Seq.empty,
        outputs = Seq.empty,
        help = maybe(op.help),
        description = maybe(op.description),
        category = op.category,
        ephemeral = op.ephemeral,
        unstable = op.unstable)
    } catch {
      case e: NoSuchElementException => throw new IllegalOpDefinition(clazz, e)
      case e: IllegalArgumentException => throw new IllegalOpDefinition(clazz, e)
    }

    OpMeta(defn, clazz.asInstanceOf[Class[Operator]])
  }

  private def maybe(str: String) = if (str.isEmpty) None else Some(str)
}