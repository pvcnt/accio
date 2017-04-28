/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.ops

import scala.collection.Set

private[ops] class SparseMatrix[U: Numeric](nbRows: Int, nbColumn: Int) {
  val numeric = implicitly[Numeric[U]]

  def nbRow = nbRows

  def nbCol = nbColumn

  var data: scala.collection.mutable.Map[(Int, Int), U] = scala.collection.mutable.Map[(Int, Int), U]()

  def indices : Set[(Int,Int)] = data.keySet

  var denominator : U  = numeric.one // stored form proportional method
  def apply(i: Int, j: Int): U = {
    if (i >= nbRow || j >= nbColumn || i < 0 || j < 0) throw new ArrayIndexOutOfBoundsException
    val d = data.get(i -> j)
    d match {
      case Some(value) => value
      case None => numeric.zero
    }
  }

  def assign(i: Int, j: Int, d: U): Unit = {
    data += (i, j) -> d
  }

  def printMatrix(): Unit = {
    println("-------------------------")
    for (i <- 0 to this.nbRow - 1) {
      for (j <- 0 to (this.nbCol - 1)) {
        print(s"${this (i, j)} \t  ")
      }
      println("")
    }
    println("-------------------------")
  }

  def printNoneEmptyCells(): Unit = {
    this.foreachNoneEmpty {
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
    if (data.keys.size < nbRow * nbCol) {
      if (numeric.gt(d, numeric.zero)) numeric.zero
      else d
    } else d
  }

  def max: U = {

    val d = if (data.isEmpty) numeric.zero else data.valuesIterator.max
    if (data.keys.size < nbRow * nbCol) {
      if (numeric.gt(numeric.zero, d)) numeric.zero
      else d
    } else d
  }

  def sum: U = {
    var s = numeric.zero
    data.foreach {
      case ((i, j), value) =>
        s = numeric.plus(s, value)
    }
    s
  }


  def div(d: U): Unit = {
    data.transform {
      case ((i, j), value) =>
        numeric match {
          case num: Fractional[U] => num.div(value, d)
          case num: Integral[U] => num.quot(value, d)
        }
    }
  }


  def sub(d: U): Unit = {

    for (i <- 0 to (this.nbRow - 2)) {
      for (j <- 0 to (this.nbCol - 2)) {
        this.sub(i, j, d)
      }
    }
  }


  def add(d: U): Unit = {
    for (i <- 0 to (this.nbRow - 2)) {
      for (j <- 0 to (this.nbCol - 2)) {
        this.add(i, j, d)
      }
    }
  }

  def mul(d: U): Unit = {
    data.transform {
      case ((i, j), value) =>
        numeric.times(value, d)
    }
  }

  def div(i: Int, j: Int, divider: U): Unit = {
    val d = data.get(i -> j)
    d match {
      case Some(value) =>
        numeric match {
          case num: Fractional[U] => data += (i, j) -> num.div(value, divider)
          case num: Integral[U] => data += (i, j) -> num.quot(value, divider)
        }
      case None =>
    }
  }

  def mul(i: Int, j: Int, multiplier: U): Unit = {
    val d = data.get(i -> j)
    d match {
      case Some(value) => data += (i, j) -> numeric.times(value, multiplier)
      case None =>
    }
  }

  def add(i: Int, j: Int, db: U): Unit = {
    val d = data.get(i -> j)
    d match {
      case Some(value) => data += (i, j) -> numeric.plus(value, db)
      case None => data += (i, j) -> numeric.plus(numeric.zero, db)
    }
  }

  def sub(i: Int, j: Int, sb: U): Unit = {
    val d = data.get(i -> j)
    d match {
      case Some(value) => data += (i, j) -> numeric.minus(value, sb)
      case None => data += (i, j) -> numeric.minus(numeric.zero, sb)
    }
  }

  def count(f: ((U) => Boolean)): Int = {
    var cpt = data.count { case (_, value) => f(value) }
    if (f(numeric.zero)) cpt += (nbRow * nbCol - data.keys.size)
    cpt
  }

  def meanZero(): Double = {
    val cpt = this.count {
      case (value) => numeric.equiv(value, numeric.zero)
    }
    //println(s" cpt =${cpt.toDouble}  ,  mul = ${(nbColumn * nbRow).toDouble}")
    cpt.toDouble / (nbColumn * nbRow).toDouble
  }

  def avg(): Double = {
    numeric.toDouble(data.valuesIterator.sum) / data.valuesIterator.size.toDouble
  }

  def diff(m: SparseMatrix[U], i: Int, j: Int): U = {
    numeric.minus(this (i, j), m(i, j))
  }

  def add(m: SparseMatrix[U], i: Int, j: Int): U = {
    numeric.plus(this (i, j), m(i, j))
  }

  def transform(f: ((Int, Int), U) => U): SparseMatrix[U] = {
    val newMat = new SparseMatrix[U](this.nbRow, this.nbCol)
    for (i <- 0 to (this.nbRow - 1)) {
      for (j <- 0 to (this.nbCol - 1)) {
        newMat.assign(i, j, f((i, j), this (i, j)))
      }
    }
    newMat
  }

  def transformCells(cells : Set[(Int,Int)],f: ((Int, Int), U) => U): SparseMatrix[U] = {
    val newMat = new SparseMatrix[U](this.nbRow, this.nbCol)
    cells.foreach{ case (i : Int,j : Int) => newMat.assign(i, j, f((i, j), this (i, j))) }
    newMat
  }



  def foreachNoneEmpty(f: (((Int, Int), U)) => Unit): Unit = {
    data.foreach(f)
  }


  def foreach(f: (((Int, Int), U)) => Unit): Unit = {
    for (i <- 0 to (this.nbRow - 1)) {
      for (j <- 0 to (this.nbCol - 1)) {
        f((i, j), this (i, j))
      }
    }
  }

  def toDouble: SparseMatrix[Double] = {
    val newMat = new SparseMatrix[Double](this.nbRow, this.nbCol)
    newMat.data = scala.collection.mutable.Map() ++ data.mapValues((value: U) => numeric.toDouble(value))
    newMat
  }

  def toInt: SparseMatrix[Int] = {
    val newMat = new SparseMatrix[Int](this.nbRow, this.nbCol)
    newMat.data = scala.collection.mutable.Map() ++ data.mapValues((value: U) => numeric.toInt(value))
    newMat
  }

  // Doest work on matrices with negative values
  def normalizePositiveMatrix: SparseMatrix[Double] = {
    val newMat = new SparseMatrix[Double](this.nbRow, this.nbCol)
    val valmax = this.max
    val valmin = this.min
    data.foreach {
      case ((i, j), value) =>
        newMat.assign(i, j, (numeric.toDouble(value) - numeric.toDouble(valmin)) / numeric.toDouble(valmax))
    }
    newMat
  }

  // Doest work on matrices with negative values
  def proportional: SparseMatrix[Double] = {
    val newMat = new SparseMatrix[Double](this.nbRow, this.nbCol)
    val sum =  this.sum
    val divider = if(numeric.equiv(sum,numeric.zero)) numeric.one else sum
    data.foreach {
      case ((i, j), value) =>
        newMat.assign(i, j, numeric.toDouble(value) / numeric.toDouble(divider))
    }
    this.denominator = divider
    newMat
  }


  def reverseProportional: SparseMatrix[Int] = {
    val newMat = new SparseMatrix[Int](this.nbRow, this.nbCol)
    data.foreach {
      case ((i, j), value) =>
        newMat.assign(i, j,   numeric.toInt(numeric.times(value,this.denominator)))
    }
    newMat
  }

  def getDenominator : Int = numeric.toInt(denominator)

  def minus(m: SparseMatrix[U]): SparseMatrix[U] = {
    val newMat = new SparseMatrix[U](this.nbRow, this.nbCol)
    val indices = this.data.keys.toSet ++ m.data.keys.toSet
    indices.foreach {
      case (i: Int, j: Int) =>
        newMat.assign(i, j, numeric.minus(this (i, j), m(i, j)))
    }
    newMat
  }

  def norm(n: Int): Double = {
    var out = numeric.zero
    data.foreach {
      case ((i, j), value) =>
        var power = numeric.one
        for (i <- 1 to n) power = numeric.times(power, value)
        out = numeric.plus(out, power)
    }
    scala.math.pow(numeric.toDouble(out), 1 / n.toDouble)
  }

  def eq(m: SparseMatrix[U]): Boolean = {
    var b = true
    val indices = this.data.keys.toSet.union(m.data.keys.toSet)
    indices.foreach {
      case (i: Int, j: Int) =>
        b = b && numeric.equiv(this (i, j), m(i, j))
    }
    b
  }

  def meanFilter(): SparseMatrix[U] = {
    val newMat = new SparseMatrix[U](this.nbRow, this.nbCol)
    for (i <- 1 to (this.nbRow - 2)) {
      for (j <- 1 to (this.nbCol - 2)) {
        for (ki <- -1 to 1) {
          for (kj <- -1 to 1) {
            newMat.add(i, j, this (i + ki, j + kj))
          }
        }
        newMat.div(i, j, numeric.fromInt(9))
      }
    }
    newMat
  }

  def carbageCollector() : SparseMatrix[U] = {
    data.filter{case (( i : Int , j : Int), value : U) => numeric.equiv(value,numeric.zero)}
    this
  }
}
