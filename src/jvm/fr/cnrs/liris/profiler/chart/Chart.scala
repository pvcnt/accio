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

/**
 * Data of a Gantt Chart to visualize the data of a profiled build.
 */
class Chart {
  /** The rows of the chart. */
  private[this] val rows = mutable.Map.empty[Long, ChartRow]

  /** The columns on the chart. */
  private[this] val columns = mutable.ListBuffer.empty[ChartColumn]

  /** The lines on the chart. */
  private[this] val lines = mutable.ListBuffer.empty[ChartLine]

  /** The types of the bars in the chart. */
  private[this] val types = mutable.Map.empty[String, ChartBarType]

  /** The running index of the rows in the chart. */
  private[this] var rowIndex = 0

  /** The maximum stop value of any bar in the chart. */
  private[this] var maxStop = 0L

  /**
   * Adds a bar to a row of the chart. If a row with the given id already
   * exists, the bar is added to the row, otherwise a new row is created and the
   * bar is added to it.
   *
   * @param id        the id of the row the new bar belongs to
   * @param start     the start value of the bar
   * @param stop      the stop value of the bar
   * @param typ       the type of the bar
   * @param label     the label of the bar
   * @param highlight emphasize the bar
   */
  def addBar(id: Long, start: Long, stop: Long, typ: ChartBarType, label: String, highlight: Boolean = false): Unit = {
    val slot = addSlotIfAbsent(id)
    val bar = ChartBar(slot, start, stop, typ, highlight, label)
    slot.addBar(bar)
    maxStop = math.max(maxStop, stop)
  }

  /**
   * Adds a vertical line to the chart.
   */
  def addVerticalLine(startId: Long, stopId: Long, pos: Long): Unit = {
    val startSlot = addSlotIfAbsent(startId)
    val stopSlot = addSlotIfAbsent(stopId)
    val line = ChartLine(startSlot, stopSlot, pos, pos)
    lines += line
  }

  /**
   * Adds a column to the chart.
   *
   * @param start the start value of the bar
   * @param stop  the stop value of the bar
   * @param typ   the type of the bar
   * @param label the label of the bar
   */
  def addTimeRange(start: Long, stop: Long, typ: ChartBarType, label: String): Unit = {
    val column = ChartColumn(start, stop, typ, label)
    columns += column
    maxStop = math.max(maxStop, stop)
  }

  /**
   * Creates a new [[ChartBarType]] and adds it to the list of types of the
   * chart.
   *
   * @param name  the name of the type
   * @param color the color of the chart
   * @return the newly created type
   */
  def createType(name: String, color: Color): ChartBarType = {
    val typ = new ChartBarType(name, color)
    types(name) = typ
    typ
  }

  /**
   * Returns the type with the given name. If no type with the given name
   * exists, a type with name 'Unknown type' is added to the chart and returned.
   *
   * @param name the name of the type to look up
   */
  def lookUpType(name: String): ChartBarType =
    types.get(name) match {
      case Some(typ) => typ
      case None =>
        val typ = ChartBarType.Unknown
        types(typ.name) = typ
        typ
    }

  /**
   * Creates a new row with the given id if no row with this id existed.
   * Otherwise the existing row with the given id is returned.
   *
   * @param id the ID of the row
   * @return the existing row, if it was already present, the newly created one
   *         otherwise
   */
  private def addSlotIfAbsent(id: Long) =
    rows.get(id) match {
      case Some(slot) => slot
      case None =>
        val slot = new ChartRow(id.toString, rowIndex)
        rows.put(id, slot)
        rowIndex += 1
        slot
    }

  /**
   * Returns the [[ChartBarType]]s, sorted by name.
   */
  def sortedTypes: Seq[ChartBarType] = types.values.toSeq.sorted

  /**
   * Returns the [[ChartRow]]s, sorted by their index.
   */
  def sortedRows: Seq[ChartRow] = rows.values.toSeq.sorted

  def getColumns: Seq[ChartColumn] = columns.toSeq

  def getLines: Seq[ChartLine] = lines.toSeq

  def rowCount: Int = rows.size

  def getMaxStop: Long = maxStop
}