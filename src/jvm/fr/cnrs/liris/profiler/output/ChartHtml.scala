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

package fr.cnrs.liris.profiler.output

import java.io.PrintStream

import fr.cnrs.liris.profiler.chart._
import fr.cnrs.liris.profiler.output.HtmlPrinter._

class ChartHtml(chart: Chart, pixelsPerSecond: Int = ChartHtml.DefaultPixelsPerSecond) {
  def print(out: PrintStream): Unit = {
    printContentBox(out)
    heading(out, "Tasks", 2)
    out.println("<p>To get more information about a task point the mouse at one of the bars.</p>")
    out.print(s"<div style='position:relative; height: ${chart.rowCount * ChartHtml.RowHeight}px; margin: ${ChartHtml.HorizontalOffset + 10}px'>\n")
    chart.sortedRows.foreach(print(out, _))
    chart.getColumns.foreach(print(out, _))
    chart.getLines.foreach(print(out, _))
    printTimeAxis(out)
    out.println("</div>")
    heading(out, "Legend", 2)
    printLegend(out)
  }

  private def print(out: PrintStream, slot: ChartRow): Unit = {
    val style = if ((slot.index % 2) == 0) "shade-even" else "shade-odd"
    val top = slot.index * ChartHtml.RowHeight
    val width = scale(chart.getMaxStop) + 1

    label(out, -ChartHtml.HorizontalOffset, top, width + ChartHtml.HorizontalOffset, ChartHtml.RowHeight, ChartHtml.RowLabelFontSize, slot.id)
    box(out, 0, top, width, ChartHtml.RowHeight, style, "", 0)

    slot.getBars.foreach(print(out, _))
  }

  private def print(out: PrintStream, bar: ChartBar): Unit = {
    val width = scale(bar.width)
    if (width != 0) {
      val left = scale(bar.start)
      val top = bar.row.index * ChartHtml.RowHeight
      var style = chartTypeNameAsCSSClass(bar.typ.name)
      if (bar.highlight) {
        style += "-highlight"
      }
      box(out, left, top + 2, width, ChartHtml.BarHeight, style, bar.label, 20)
    }
  }

  private def print(out: PrintStream, column: ChartColumn): Unit = {
    val width = scale(column.width)
    if (width != 0) {
      val left = scale(column.start)
      val height = chart.rowCount * ChartHtml.RowHeight
      val style = chartTypeNameAsCSSClass(column.typ.name)
      box(out, left, 0, width, height, style, column.label, 10)
    }
  }

  private def print(out: PrintStream, line: ChartLine): Unit = {
    val start = line.startRow.index * ChartHtml.RowHeight
    val stop = line.stopRow.index * ChartHtml.RowHeight
    val time = scale(line.startTime)

    if (start < stop) {
      verticalLine(out, time, start + 1, 1, (stop - start) + ChartHtml.RowHeight, Color.Red)
    } else {
      verticalLine(out, time, stop + 1, 1, (start - stop) + ChartHtml.RowHeight, Color.Red)
    }
  }

  /**
   * Converts the given value from the bar of the chart to pixels.
   */
  private def scale(value: Long) = (value / (1000000000L / pixelsPerSecond)).toInt

  /**
   * Prints a box with links to the sections of the generated HTML document.
   */
  private def printContentBox(out: PrintStream): Unit = {
    out.println("<div style='position:fixed; top:1em; right:1em; z-index:50; padding: 1ex;"
        + "border:1px solid #888; background-color:#eee; width:100px'><h3>Content</h3>")
    out.println("<p style='text-align:left;font-size:small;margin:2px'>"
        + "<a href='#Tasks'>Tasks</a></p>")
    out.println("<p style='text-align:left;font-size:small;margin:2px'>"
        + "<a href='#Legend'>Legend</a></p>")
    out.println("<p style='text-align:left;font-size:small;margin:2px'>"
        + "<a href='#Statistics'>Statistics</a></p></div>")
  }

  /**
   * Prints the legend for the chart at the current position in the document. The
   * legend is printed in columns of 10 rows each.
   */
  private def printLegend(out: PrintStream): Unit = {
    val boxHeight = 20
    val lineHeight = 25
    val entriesPerColumn = 10
    val legendWidth = 350
    val types = chart.sortedTypes
    val legendHeight =
      if (types.size / entriesPerColumn >= 1) {
        entriesPerColumn
      } else {
        types.size % entriesPerColumn
      }

    out.print(s"<div style='position:relative; height: ${(legendHeight + 1) * lineHeight}px;'>")

    var left = -legendWidth
    var i = 0
    types.foreach { typ =>
      if (i % entriesPerColumn == 0) {
        left += legendWidth
        i = 0
      }
      val top = lineHeight * i
      val style = chartTypeNameAsCSSClass(typ.name) + "-border"
      box(out, left, top, boxHeight, boxHeight, style, typ.name, 0)
      label(out, left + lineHeight + 10, top, legendWidth - 10, boxHeight, 0, typ.name)
      i += 1
    }
    out.println("</div>")
    out.element("div", "style", "clear:both")
  }

  /**
   * Prints the time axis of the chart and vertical lines for every second.
   */
  private def printTimeAxis(out: PrintStream) = {
    var location = 0
    var second = 0
    val end = scale(chart.getMaxStop)
    while (location < end) {
      label(out, location + 4, -17, pixelsPerSecond, ChartHtml.RowHeight, 0, second + "s")
      verticalLine(out, location, -20, 1, chart.rowCount * ChartHtml.RowHeight + 20, Color.Gray)
      location += pixelsPerSecond
      second += 1
    }
  }

  def printCss(out: PrintStream): Unit = {
    out.println("""<style type="text/css"><!--""")
    out.println("body { font-family: Sans; }")
    out.println("div.shade-even { position:absolute; border: 0px; background-color:#dddddd }")
    out.println("div.shade-odd { position:absolute; border: 0px; background-color:#eeeeee }")
    chart.sortedTypes.foreach { typ =>
      val name = chartTypeNameAsCSSClass(typ.name)
      val color = formatColor(typ.color)
      out.println(s"div.$name-border { position:absolute; border:1px solid grey; background-color:$color }")
      out.println(s"div.$name-highlight { position:absolute; border:1px solid red; background-color:$color }")
      out.println(s"div.$name { position:absolute; border:0px; margin:1px; background-color:$color }")
    }
    out.println("--></style>")
  }

  /**
   * Prints a head-line at the current position in the document.
   *
   * @param text  the text to print
   * @param level the headline level
   */
  private def heading(out: PrintStream, text: String, level: Int) = {
    anchor(out, text)
    out.print(s"<h$level >$text</h$level>\n")
  }

  /**
   * Prints a box with the given location, size, background color and border.
   *
   * @param x      the x location of the top left corner of the box
   * @param y      the y location of the top left corner of the box
   * @param width  the width location of the box
   * @param height the height location of the box
   * @param style  the CSS style class to use for the box
   * @param title  the text displayed when the mouse hovers over the box
   */
  private def box(out: PrintStream, x: Int, y: Int, width: Int, height: Int, style: String, title: String, zIndex: Int) = {
    out.println(s"""<div class="$style" title="$title" style="left:${x}px; top:${y}px; width:${width}px; height:${height}px; z-index:$zIndex"></div>""")
  }

  /**
   * Prints a label with the given location, size, background color and border.
   *
   * @param x        the x location of the top left corner of the box
   * @param y        the y location of the top left corner of the box
   * @param width    the width location of the box
   * @param height   the height location of the box
   * @param fontSize the font size of text in the box, 0 for default
   * @param text     the text displayed in the box
   */
  private def label(out: PrintStream, x: Int, y: Int, width: Int, height: Int, fontSize: Int, text: String) = {
    out.print(s"""<div style="position:absolute; left:${x}px; top:${y}px; width:${width}px; height:${height}px;""")
    if (fontSize > 0) {
      out.print(s"font-size:${fontSize}pt")
    }
    out.println(s"""">$text</div>""")
  }

  /**
   * Prints a vertical line of given width, height and color at the given
   * location.
   *
   * @param x      the x location of the start point of the line
   * @param y      the y location of the start point of the line
   * @param width  the width of the line
   * @param length the length of the line
   * @param color  the color of the line
   */
  private def verticalLine(out: PrintStream, x: Int, y: Int, width: Int, length: Int, color: Color) = {
    out.println(
      s"""<div style="position: absolute; left: ${x}px; top: ${y}px; width: ${width}px;
          | height: ${length}px; border-left: ${width}px solid ${formatColor(color)}"></div>""".stripMargin)
  }

  /**
   * Prints an HTML anchor with the given name,
   */
  private def anchor(out: PrintStream, name: String) = {
    out.println(s"""<a name="$name"></a>""")
  }

  /**
   * Transform the name into a form suitable as a css class.
   */
  private def chartTypeNameAsCSSClass(name: String) = name.replace(' ', '_')

  /**
   * Formats the given [[Color]] to a css style color string.
   */
  private def formatColor(color: Color) =
    s"rgba(${color.red},${color.green},${color.blue},${color.alpha / 255.0})"
}

object ChartHtml {
  val DefaultPixelsPerSecond = 30

  /** The horizontal offset of second zero. */
  private val HorizontalOffset = 40

  /** The font size of the row labels. */
  private val RowLabelFontSize = 7

  /** The height of a bar in pixels. */
  private val BarHeight = 8

  /** The space between two bars in pixels. */
  private val BarSpace = 2

  /** The height of a row. */
  private val RowHeight = BarHeight + BarSpace
}