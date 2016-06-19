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

package fr.cnrs.liris.profiler.chart

import scala.collection.mutable

sealed trait ChartNode

/**
 * A chart column. The column can be used to highlight a time-range.
 *
 * @param start value of the bar. This value has no unit. The interpretation of
 *              the value is up to the user of the class.
 * @param stop  stop value of the bar. This value has no unit. The interpretation of
 *              the value is up to the user of the class.
 * @param typ   The type of the bar
 * @param label label of the bar
 */
case class ChartColumn(start: Long, stop: Long, typ: ChartBarType, label: String) extends ChartNode {
  def width: Long = stop - start
}

/**
 * A chart line. Such lines can be used to connect boxes.
 *
 * @param startRow  the start row
 * @param stopRow   the end row
 * @param startTime start time
 * @param stopTime  end time
 */
case class ChartLine(startRow: ChartRow, stopRow: ChartRow, startTime: Long, stopTime: Long)
    extends ChartNode

/**
 * A row of a Gantt Chart. A chart row is identified by its id and has an index that
 * determines its location in the chart.
 *
 * @param id    Unique id of this row
 * @param index Index, i.e., the row number of the row in the chart
 */
class ChartRow(val id: String, val index: Int) extends ChartNode with Ordered[ChartRow] {
  private[this] val bars = mutable.ListBuffer.empty[ChartBar]

  /**
   * Adds a bar to the chart row.
   *
   * @param bar A bar to add
   */
  def addBar(bar: ChartBar): Unit = {
    bars += bar
  }

  /**
   * Return the bars of the row.
   */
  def getBars: Seq[ChartBar] = bars.toSeq

  override def compare(other: ChartRow): Int = index.compare(other.index)
}

/**
 * A bar in a row of a Gantt Chart.
 *
 * @param row       the chart row this bar belongs to
 * @param start     the start value of the bar. This value has no unit. The interpretation of
 *                  the value is up to the user of the class.
 * @param stop      the stop value of the bar. This value has no unit. The interpretation of
 *                  the value is up to the user of the class.
 * @param typ       the type of the bar
 * @param highlight Emphasize the bar
 * @param label     the label of the bar
 */
case class ChartBar(row: ChartRow, start: Long, stop: Long, typ: ChartBarType, highlight: Boolean, label: String)
    extends ChartNode {
  def width: Long = stop - start
}

/**
 * The type of a bar in a Gantt Chart. A type consists of a name and a color.
 * Types are used to create the legend of a Gantt Chart.
 *
 * @param name  The name of the type
 * @param color The color of the type
 */
class ChartBarType(val name: String, val color: Color) extends Ordered[ChartBarType] {
  override def hashCode: Int = name.hashCode

  override def equals(other: Any): Boolean = other match {
    case o: ChartBarType => o.name == name
    case _ => false
  }

  override def compare(other: ChartBarType): Int = name.compare(other.name)
}

object ChartBarType {
  /** The type that is returned when an unknown type is looked up. */
  val Unknown = new ChartBarType("Unknown type", Color.Red)
}