package fr.cnrs.liris.common.io

/**
 * Wrap an [[AnsiTerminalWriter]] into one that breaks lines to use
 * at most a first given number of columns of the terminal. In this way,
 * all line breaks are predictable, even if we only have a lower bound
 * on the number of columns of the underlying terminal. To simplify copy
 * and paste of the terminal output, a continuation character is written
 * into the last usable column when we break a line. Additionally, newline
 * characters are translated into calls to the [[AnsiTerminalWriter.newline]]
 * method.
 */
class LineWrappingAnsiTerminalWriter(terminalWriter: AnsiTerminalWriter, width: Int, continuationChar: Char = '\\') extends AnsiTerminalWriter {
  private[this] var position = 0

  override def append(text: String): AnsiTerminalWriter = {
    text.foreach(appendChar)
    this
  }

  override def newline(): AnsiTerminalWriter = {
    terminalWriter.newline()
    position = 0
    this
  }

  override def okStatus(): AnsiTerminalWriter = {
    terminalWriter.okStatus()
    this
  }

  override def failStatus(): AnsiTerminalWriter = {
    terminalWriter.failStatus()
    this
  }

  override def normal(): AnsiTerminalWriter = {
    terminalWriter.normal()
    this
  }

  private def appendChar(c: Char) = {
    if (c == '\n') {
      terminalWriter.newline()
      position = 0
    } else if (position + 1 < width) {
      terminalWriter.append(Character.toString(c))
      position += 1
    } else {
      // The last usable character of the line was already been written,
      // hence we have to start a continuation before writing the symbol.
      terminalWriter.append(Character.toString(continuationChar))
      terminalWriter.newline()
      terminalWriter.append(Character.toString(c))
      position = 1
    }
  }
}