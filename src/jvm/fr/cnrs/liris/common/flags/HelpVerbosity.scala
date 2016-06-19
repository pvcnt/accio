package fr.cnrs.liris.common.flags

/**
 * The verbosity with which option help messages are displayed.
 */
sealed class HelpVerbosity(protected val level: Int) extends Ordered[HelpVerbosity] {
  override def compare(that: HelpVerbosity): Int = level.compareTo(that.level)
}

object HelpVerbosity {

  /**
   * Display only the name of the flags.
   */
  case object Short extends HelpVerbosity(0)

  /**
   * Display the name, type and default value of the flags.
   */
  case object Medium extends HelpVerbosity(1)

  /**
   * Display the full description of the flags.
   */
  case object Long extends HelpVerbosity(2)

}
