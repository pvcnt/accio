package fr.cnrs.liris.accio.core.framework

import java.util.NoSuchElementException

import fr.cnrs.liris.accio.core.param.Param
import fr.cnrs.liris.common.reflect.CaseClass

import scala.reflect.ClassTag

/**
 * Metadata about an operator.
 *
 * @param defn  Operator definition
 * @param clazz Operator class
 */
case class OpMeta(defn: OperatorDef, clazz: Class[_ <: Operator[_, _]])

/**
 * Exception thrown when the definition of an operator is invalid.
 *
 * @param clazz Operator class
 * @param cause Root exception
 */
class IllegalOpDefinition(clazz: Class[_ <: Operator[_, _]], cause: Throwable)
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
  def read[T <: Operator[_, _] : ClassTag]: OpMeta = read(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])

  def read[T <: Operator[_, _]](clazz: Class[T]): OpMeta
}

/**
 * Reads operator metadata from annotations found on this operator type.
 */
class AnnotationOpMetaReader extends OpMetaReader {
  override def read[T <: Operator[_, _]](clazz: Class[T]): OpMeta = {
    val defn = try {
      val refl = CaseClass(clazz)
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
        unstable = op.unstable)
    } catch {
      case e: NoSuchElementException => throw new IllegalOpDefinition(clazz, e)
      case e: IllegalArgumentException => throw new IllegalOpDefinition(clazz, e)
    }
    OpMeta(defn, clazz)
  }

  private def getParams(refl: CaseClass) =
    refl.fields.map { field =>
      require(field.isAnnotated[Param], s"Operator parameter ${field.name} must be annotated with @Param")
      val param = field.annotation[Param]
      val tpe = if (field.isOption) field.scalaType.typeArguments.head else field.scalaType
      ParamDef(field.name, ParamType(tpe), maybe(param.help), field.defaultValue, field.isOption)
    }

  private def getInputs(refl: CaseClass): Seq[InputDef] =
    refl.runtimeClass match {
      case t if classOf[Transformer].isAssignableFrom(t) => Seq(InputDef("data", Some("Input dataset of traces")))
      case t if classOf[Analyzer[_, _]].isAssignableFrom(t) => Seq(InputDef("data", Some("Input dataset of traces")))
      case t if classOf[Evaluator[_, _]].isAssignableFrom(t) => Seq(
        InputDef("train", Some("Training dataset of traces")),
        InputDef("test", Some("Testing dataset of traces")))
      case _ => Seq.empty[InputDef]
    }

  private def getOutputs(refl: CaseClass, op: Op): Seq[OutputDef] =
    refl.runtimeClass match {
      case t if classOf[Transformer].isAssignableFrom(t) =>
        Seq(OutputDef("data", "dataset", Some("Output dataset of traces")))
      case t if classOf[Analyzer[_, _]].isAssignableFrom(t) =>
        op.metrics.map(name => OutputDef(name, "distribution", None)).toSeq
      case t if classOf[Evaluator[_, _]].isAssignableFrom(t) =>
        op.metrics.map(name => OutputDef(name, "distribution", None)).toSeq
      case t if classOf[Source[_]].isAssignableFrom(t) =>
        Seq(OutputDef("data", "dataset", Some("Source dataset of traces")))
    }

  private def maybe(str: String) = if (str.isEmpty) None else Some(str)
}