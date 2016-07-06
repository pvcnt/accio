package fr.cnrs.liris.accio.core.framework

import java.util.NoSuchElementException

import fr.cnrs.liris.accio.core.param.Param
import fr.cnrs.liris.common.reflect.ReflectCaseClass

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * Metadata about an operator.
 *
 * @param defn  Operator definition
 * @param clazz Operator class
 */
case class OpMeta(defn: OperatorDef, clazz: Class[Operator[_, _]])

/**
 * Exception thrown when the definition of an operator is invalid.
 *
 * @param clazz Operator class
 * @param cause Root exception
 */
class IllegalOpDefinition(clazz: Class[Operator[_, _]], cause: Throwable)
    extends Exception(s"Illegal definition of operator ${clazz.getName}: ${cause.getMessage}", cause)

/**
 * Metadata readers extract metadata about operators.
 */
trait OpMetaReader {
  /**
   * Read operator metadata from its class specification.
   *
   * @tparam T Operator type
   * @return Operator metadata
   * @throws IllegalOpDefinition If the operator definition is invalid
   */
  @throws[IllegalOpDefinition]
  def read[T <: Operator[_, _] : ClassTag : TypeTag]: OpMeta
}

/**
 * Reads operator metadata from annotations found on this operator type.
 */
class AnnotationOpMetaReader extends OpMetaReader {
  override def read[T <: Operator[_, _] : ClassTag : TypeTag]: OpMeta = {
    val clazz = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[Operator[_, _]]]
    val defn = try {
      val refl = ReflectCaseClass.of[T]
      require(refl.isAnnotated[Op], s"Operator must be annotated with @Op")
      val op = refl.annotation[Op]
      val name = if (op.name.nonEmpty) op.name else clazz.getSimpleName.stripSuffix("Op")
      OperatorDef(
        name = name,
        params = getParams(refl),
        inputs = getInputs(refl),
        outputs = getOutputs(refl, op),
        help = maybe(op.help),
        description = maybe(op.description),
        category = op.category,
        ephemeral = op.ephemeral,
        unstable = op.unstable)
    } catch {
      case e: NoSuchElementException => throw new IllegalOpDefinition(clazz, e)
      case e: IllegalArgumentException => throw new IllegalOpDefinition(clazz, e)
    }
    OpMeta(defn, clazz.asInstanceOf[Class[Operator[_, _]]])
  }

  private def getParams(refl: ReflectCaseClass) =
    refl.fields.map { field =>
      require(field.isAnnotated[Param], s"Operator parameter ${field.name} must be annotated with @Param")
      val param = field.annotation[Param]
      val optional = field.tpe <:< typeOf[Option[_]]
      val tpe = if (optional) field.tpe.baseType(typeOf[Option[_]].typeSymbol).typeArgs.head else field.tpe
      ParamDef(field.name, ParamType.of(tpe), maybe(param.help), field.defaultValue, optional)
    }

  private def getInputs(refl: ReflectCaseClass): Seq[InputDef] =
    refl.tpe match {
      case t if t <:< typeOf[Transformer] => Seq(InputDef("data", Some("Input dataset of traces")))
      case t if t <:< typeOf[Analyzer[_, _]] => Seq(InputDef("data", Some("Input dataset of traces")))
      case t if t <:< typeOf[Evaluator[_, _]] => Seq(
        InputDef("train", Some("Training dataset of traces")),
        InputDef("test", Some("Testing dataset of traces")))
      case _ => Seq.empty[InputDef]
    }

  private def getOutputs(refl: ReflectCaseClass, op: Op): Seq[OutputDef] =
    refl.tpe match {
      case t if t <:< typeOf[Transformer] =>
        Seq(OutputDef("data", "dataset", Some("Output dataset of traces")))
      case t if t <:< typeOf[Analyzer[_, _]] =>
        op.metrics.map(name => OutputDef(name, "distribution", None)).toSeq
      case t if t <:< typeOf[Evaluator[_, _]] =>
        op.metrics.map(name => OutputDef(name, "distribution", None)).toSeq
      case t if t <:< typeOf[Source[_]] =>
        Seq(OutputDef("data", "dataset", Some("Source dataset of traces")))
    }

  private def maybe(str: String) = if (str.isEmpty) None else Some(str)
}