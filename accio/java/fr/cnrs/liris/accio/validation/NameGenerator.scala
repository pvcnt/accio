/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.accio.validation

import java.math.BigInteger
import java.util.UUID

trait NameGenerator {
  def generateName(): String
}

class UUIDNameGenerator extends NameGenerator {
  override def generateName(): String = UUID.randomUUID().toString.replaceAll("-", "")
}

object UUIDNameGenerator extends UUIDNameGenerator

class ShortNameGenerator extends NameGenerator {
  // https://github.com/hsingh/java-shortuuid
  private[this] val alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray
  private[this] val alphabetSize = alphabet.length

  override def generateName(): String = {
    val uuid = UUIDNameGenerator.generateName()
    val factor = math.log(25d) / math.log(alphabetSize)
    val length = math.ceil(factor * 16)
    val number = new BigInteger(uuid, 16)
    encode(number, length.toInt)
  }

  private def encode(bigInt: BigInteger, padToLen: Int): String = {
    var value = new BigInteger(bigInt.toString())
    val alphaSize = BigInteger.valueOf(alphabetSize)
    val shortUuid = new StringBuilder()

    while (value.compareTo(BigInteger.ZERO) > 0) {
      val fracAndRemainder = value.divideAndRemainder(alphaSize)
      shortUuid.append(alphabet(fracAndRemainder.last.intValue))
      value = fracAndRemainder.head
    }

    if (padToLen > 0) {
      val padding = math.max(padToLen - shortUuid.length(), 0)
      (0 until padding).foreach(shortUuid.append(alphabet.head))
    }

    shortUuid.toString
  }
}

object ShortNameGenerator extends ShortNameGenerator