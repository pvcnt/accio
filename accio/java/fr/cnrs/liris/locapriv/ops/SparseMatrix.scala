/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016 Vincent Primault <v.primault@ucl.ac.uk>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnrs.liris.locapriv.ops

import scala.collection.Set

private[ops] class SparseMatrix[U: Numeric](val nbRows: Int, val nbCols: Int) {
  private[this] val numeric = implicitly[Numeric[U]]
  private[this] var denominator: U = numeric.one // stored form proportional method
  private[ops] var data: scala.collection.mutable.Map[(Int, Int), U] = scala.collection.mutable.Map[(Int, Int), U]()

  def indices: Set[(Int, Int)] = data.keySet

  def apply(i: Int, j: Int): U = {
    if (i >= nbRows || j >= nbCols || i < 0 || j < 0) throw new ArrayIndexOutOfBoundsException
    data.get(i -> j) match {
      case Some(value) => value
      case None => numeric.zero
    }
  }

  def assign(i: Int, j: Int, d: U): Unit = {
    data += (i, j) -> d
  }

  def printMatrix(): Unit = {
    println("-------------------------")
    for (i <- 0 until nbRows) {
      for (j <- 0 until nbCols) {
        print(s"${this (i, j)} \t  ")
      }
      println("")
    }
    println("-------------------------")
  }

  def printNoneEmptyCells(): Unit = {
    foreachNoneEmpty {
      case ((i, j), v) =>
        println(s" ($i,$j) = $v")
    }
  }

  def inc(i: Int, j: Int): Unit = {
    val d = data.get(i -> j)
    d match {
      case Some(value) => data += (i, j) -> numeric.plus(value, numeric.one)
      case None => data += (i, j) -> numeric.one
    }
  }

  def min: U = {
    val d = if (data.isEmpty) numeric.zero else data.valuesIterator.min
    if (data.keys.size < nbRows * nbCols) {
      if (numeric.gt(d, numeric.zero)) numeric.zero else d
    } else d
  }

  def max: U = {
    val d = if (data.isEmpty) numeric.zero else data.valuesIterator.max
    if (data.keys.size < nbRows * nbCols) {
      if (numeric.gt(numeric.zero, d)) numeric.zero else d
    } else d
  }

  def sum: U = data.values.foldLeft(numeric.zero)(numeric.plus)

  def div(d: U): Unit = {
    data.transform { case (_, value) =>
      numeric match {
        case num: Fractional[U] => num.div(value, d)
        case num: Integral[U] => num.quot(value, d)
      }
    }
  }

  def sub(d: U): Unit = {
    for (
      i <- 0 to (nbRows - 2);
      j <- 0 to (nbCols - 2)) {
      sub(i, j, d)
    }
  }

  def add(d: U): Unit = {
    for (i <- 0 to (nbRows - 2)) {
      for (j <- 0 to (nbCols - 2)) {
        add(i, j, d)
      }
    }
  }

  def mul(d: U): Unit = {
    data.transform { case (_, value) =>
      numeric.times(value, d)
    }
  }

  def div(i: Int, j: Int, divider: U): Unit = {
    data.get(i -> j) match {
      case Some(value) =>
        numeric match {
          case num: Fractional[U] => data += (i, j) -> num.div(value, divider)
          case num: Integral[U] => data += (i, j) -> num.quot(value, divider)
        }
      case None =>
    }
  }

  def mul(i: Int, j: Int, multiplier: U): Unit = {
    data.get(i -> j) match {
      case Some(value) => data += (i, j) -> numeric.times(value, multiplier)
      case None =>
    }
  }

  def add(i: Int, j: Int, db: U): Unit = {
    data.get(i -> j) match {
      case Some(value) => data += (i, j) -> numeric.plus(value, db)
      case None => data += (i, j) -> numeric.plus(numeric.zero, db)
    }
  }

  def sub(i: Int, j: Int, sb: U): Unit = {
    data.get(i -> j) match {
      case Some(value) => data += (i, j) -> numeric.minus(value, sb)
      case None => data += (i, j) -> numeric.minus(numeric.zero, sb)
    }
  }

  def count(f: ((U) => Boolean)): Int = {
    val cpt = data.values.count(f)
    if (f(numeric.zero)) cpt + (nbRows * nbCols - data.keys.size) else cpt
  }

  def meanZero(): Double = {
    val cpt = count(numeric.equiv(_, numeric.zero))
    cpt.toDouble / (nbCols * nbRows).toDouble
  }

  def avg(): Double = numeric.toDouble(data.valuesIterator.sum) / data.valuesIterator.size.toDouble

  def diff(m: SparseMatrix[U], i: Int, j: Int): U = numeric.minus(apply(i, j), m(i, j))

  def add(m: SparseMatrix[U], i: Int, j: Int): U = numeric.plus(apply(i, j), m(i, j))

  def transform(f: ((Int, Int), U) => U): SparseMatrix[U] = {
    val newMat = new SparseMatrix[U](nbRows, nbCols)
    for (i <- 0 until nbRows) {
      for (j <- 0 until nbCols) {
        newMat.assign(i, j, f((i, j), apply(i, j)))
      }
    }
    newMat
  }

  def transformCells(cells: Set[(Int, Int)], f: ((Int, Int), U) => U): SparseMatrix[U] = {
    val newMat = new SparseMatrix[U](nbRows, nbCols)
    cells.foreach { case (i, j) => newMat.assign(i, j, f((i, j), apply(i, j))) }
    newMat
  }


  def foreachNoneEmpty(f: (((Int, Int), U)) => Unit): Unit = data.foreach(f)


  def foreach(f: (((Int, Int), U)) => Unit): Unit = {
    for (
      i <- 0 until nbRows;
      j <- 0 until nbCols) {
      f((i, j), apply(i, j))
    }
  }

  def toDouble: SparseMatrix[Double] = {
    val newMat = new SparseMatrix[Double](nbRows, nbCols)
    newMat.data = scala.collection.mutable.Map() ++ data.mapValues((value: U) => numeric.toDouble(value))
    newMat
  }

  def toInt: SparseMatrix[Int] = {
    val newMat = new SparseMatrix[Int](nbRows, nbCols)
    newMat.data = scala.collection.mutable.Map() ++ data.mapValues((value: U) => numeric.toInt(value))
    newMat
  }

  // Doest work on matrices with negative values
  def normalizePositiveMatrix: SparseMatrix[Double] = {
    val newMat = new SparseMatrix[Double](nbRows, nbCols)
    val valmax = max
    val valmin = min
    data.foreach { case ((i, j), value) =>
      newMat.assign(i, j, (numeric.toDouble(value) - numeric.toDouble(valmin)) / numeric.toDouble(valmax))
    }
    newMat
  }

  // Doest work on matrices with negative values
  def proportional: SparseMatrix[Double] = {
    val newMat = new SparseMatrix[Double](nbRows, nbCols)
    val s = sum
    val divider = if (numeric.equiv(s, numeric.zero)) numeric.one else s
    data.foreach { case ((i, j), value) =>
      newMat.assign(i, j, numeric.toDouble(value) / numeric.toDouble(divider))
    }
    denominator = divider
    newMat
  }


  def reverseProportional: SparseMatrix[Int] = {
    val newMat = new SparseMatrix[Int](nbRows, nbCols)
    data.foreach { case ((i, j), value) =>
      newMat.assign(i, j, numeric.toInt(numeric.times(value, denominator)))
    }
    newMat
  }

  def getDenominator: Int = numeric.toInt(denominator)

  def minus(m: SparseMatrix[U]): SparseMatrix[U] = {
    val newMat = new SparseMatrix[U](nbRows, nbCols)
    val indices = data.keys.toSet ++ m.data.keys.toSet
    indices.foreach { case (i, j) =>
      newMat.assign(i, j, numeric.minus(apply(i, j), m(i, j)))
    }
    newMat
  }

  def norm(n: Int): Double = {
    var out = numeric.zero
    data.foreach { case (_, value) =>
      var power = numeric.one
      for (_ <- 1 to n) power = numeric.times(power, value)
      out = numeric.plus(out, power)
    }
    math.pow(numeric.toDouble(out), 1 / n.toDouble)
  }

  def eq(m: SparseMatrix[U]): Boolean = {
    //TODO: eq method is already defined on Scala objects, with a different meaning!
    var b = true
    val indices = data.keys.toSet.union(m.data.keys.toSet)
    indices.foreach { case (i, j) =>
      b = b && numeric.equiv(apply(i, j), m(i, j))
    }
    b
  }

  def meanFilter(): SparseMatrix[U] = {
    val newMat = new SparseMatrix[U](nbRows, nbCols)
    for (i <- 1 to (nbRows - 2)) {
      for (j <- 1 to (nbCols - 2)) {
        for (ki <- -1 to 1) {
          for (kj <- -1 to 1) {
            newMat.add(i, j, apply(i + ki, j + kj))
          }
        }
        newMat.div(i, j, numeric.fromInt(9))
      }
    }
    newMat
  }

  def garbageCollector(): SparseMatrix[U] = {
    //TODO: does nothing!
    data.filter { case (_, value) => numeric.equiv(value, numeric.zero) }
    this
  }
}
