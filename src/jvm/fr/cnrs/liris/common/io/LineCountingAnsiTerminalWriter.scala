package fr.cnrs.liris.common.io

/**
 * Class providing the AnsiTerminalWriter interface from a terminal while additionally counting the number of
 * written lines.
 */
class LineCountingAnsiTerminalWriter(terminal: AnsiTerminal) extends AnsiTerminalWriter {
  private[this] var lineCount = 0

  override def append(text: String): AnsiTerminalWriter = {
    terminal.writeString(text)
    this
  }

  override def newline(): AnsiTerminalWriter = {
    terminal.cr()
    terminal.writeString("\n")
    lineCount += 1
    this
  }

  override def okStatus(): AnsiTerminalWriter = {
    terminal.textGreen()
    this
  }

  override def failStatus(): AnsiTerminalWriter = {
    terminal.textRed()
    terminal.textBold()
    this
  }

  override def normal(): AnsiTerminalWriter = {
    terminal.resetTerminal()
    this
  }

  def writtenLines: Int = lineCount
}