package fr.cnrs.liris.common.flags

import java.nio.file.{Path, Paths}
import org.joda.time.{Duration => JodaDuration}

import com.twitter.util.{Duration => TwitterDuration}
import fr.cnrs.liris.common.util.Distance

import scala.collection.mutable
import scala.reflect.runtime.universe._

/**
 * A converter is a little helper object that can take a String and turn it into an instance of
 * type T (the type parameter to the converter).
 */
trait Converter[T] {
  /**
   * Convert a string into type T.
   */
  @throws[FlagsParsingException]
  def convert(input: String): T

  /**
   * The type description appears in usage messages. E.g.: "a string", "a path", etc.
   */
  def typeDescription: String
}

object Converter {
  private[this] val registry = mutable.Map.empty[Type, Converter[_]]

  def of(tpe: Type): Converter[_] = {
    registry.find { case (key, _) => key =:= tpe }.map(_._2) match {
      case Some(converter) => converter
      case None => throw new NoSuchElementException(s"No converter available for $tpe")
    }
  }

  def of[T: TypeTag]: Converter[T] = of(typeOf[T]).asInstanceOf[Converter[T]]

  def register[T: TypeTag](converter: Converter[T]): Unit = {
    val tpe = typeOf[T]
    require(!registry.contains(tpe),
      s"Duplicate converter for $tpe (found: ${registry(tpe).getClass.getName}, rejected: ${converter.getClass.getName})")
    registry(tpe) = converter
  }

  register(new Converter[String] {
    override def convert(input: String): String = input

    override def typeDescription: String = "a string"
  })

  register(new Converter[Int] {
    override def convert(input: String): Int = try {
      input.toInt
    } catch {
      case _: NumberFormatException => throw new FlagsParsingException(s"'$input' is not an integer")
    }

    override def typeDescription: String = "an integer"
  })

  register(new Converter[Long] {
    override def convert(input: String): Long = try {
      input.toLong
    } catch {
      case _: NumberFormatException => throw new FlagsParsingException(s"'$input' is not an integer")
    }

    override def typeDescription: String = "an integer"
  })

  register(new Converter[Double] {
    override def convert(input: String): Double = try {
      input.toDouble
    } catch {
      case _: NumberFormatException => throw new FlagsParsingException(s"'$input' is not a double")
    }

    override def typeDescription: String = "a double"
  })

  register(new Converter[Path] {
    override def convert(input: String): Path = Paths.get(input)

    override def typeDescription: String = "a path"
  })

  register(new Converter[Distance] {
    override def convert(input: String): Distance = try {
      Distance.parse(input)
    } catch {
      case _: NumberFormatException => throw new FlagsParsingException(s"'$input' is not a distance")
    }

    override def typeDescription: String = "a distance"
  })

  register(new Converter[TwitterDuration] {
    override def convert(input: String): TwitterDuration = try {
      TwitterDuration.parse(input)
    } catch {
      case e: NumberFormatException =>
        throw new FlagsParsingException(s"'$input' is not a duration", None, e)
    }

    override def typeDescription: String = "a duration"
  })

  register(new Converter[JodaDuration] {
    override def convert(input: String): JodaDuration = {
      JodaDuration.millis(TwitterDuration.parse(input).inMillis)
    }

    override def typeDescription: String = "a duration"
  })

  register(new Converter[Boolean] {
    override def convert(input: String): Boolean = input.toLowerCase match {
      case "true" | "t" | "1" | "yes" | "y" => true
      case "false" | "f" | "0" | "no" | "n" => false
      case _ => throw new FlagsParsingException(s"'$input' is not a boolean")
    }

    override def typeDescription: String = "a boolean"
  })

  register(new Converter[TriState] {
    override def convert(input: String): TriState = input.toLowerCase match {
      case "auto" => TriState.Auto
      case _ => if (Converter.of[Boolean].convert(input)) TriState.Yes else TriState.No
    }

    override def typeDescription: String = "a tri-state (auto, yes, no)"
  })
}