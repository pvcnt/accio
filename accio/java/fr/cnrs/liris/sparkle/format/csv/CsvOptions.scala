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

package fr.cnrs.liris.sparkle.format.csv

import com.univocity.parsers.csv.{CsvParserSettings, CsvWriterSettings, UnescapedQuoteHandling}

case class CsvOptions(
  delimiter: Char,
  quote: Char,
  escape: Char,
  nullValue: String,
  nanValue: String,
  positiveInf: String,
  negativeInf: String,
  timestampFormat: String) {

  def asWriterSettings: CsvWriterSettings = {
    val writerSettings = new CsvWriterSettings()
    val format = writerSettings.getFormat
    format.setDelimiter(delimiter)
    format.setQuote(quote)
    format.setQuoteEscape(escape)
    format.setLineSeparator("\n")
    //charToEscapeQuoteEscaping.foreach(format.setCharToEscapeQuoteEscaping)
    //format.setComment(comment)
    //writerSettings.setIgnoreLeadingWhitespaces(ignoreLeadingWhiteSpaceFlagInWrite)
    //writerSettings.setIgnoreTrailingWhitespaces(ignoreTrailingWhiteSpaceFlagInWrite)
    writerSettings.setNullValue(nullValue)
    writerSettings.setEmptyValue(nullValue)
    writerSettings.setSkipEmptyLines(true)
    //writerSettings.setQuoteAllFields(quoteAll)
    //writerSettings.setQuoteEscapingEnabled(escapeQuotes)
    writerSettings
  }

  def asParserSettings: CsvParserSettings = {
    val settings = new CsvParserSettings()
    val format = settings.getFormat
    format.setDelimiter(delimiter)
    format.setQuote(quote)
    format.setQuoteEscape(escape)
    format.setLineSeparator("\n")
    settings.setHeaderExtractionEnabled(true)
    //charToEscapeQuoteEscaping.foreach(format.setCharToEscapeQuoteEscaping)
    //settings.setIgnoreLeadingWhitespaces(ignoreLeadingWhiteSpaceInRead)
    //settings.setIgnoreTrailingWhitespaces(ignoreTrailingWhiteSpaceInRead)
    settings.setReadInputOnSeparateThread(false)
    //settings.setInputBufferSize(inputBufferSize)
    //settings.setMaxColumns(maxColumns)
    settings.setNullValue(nullValue)
    //settings.setMaxCharsPerColumn(maxCharsPerColumn)
    settings.setUnescapedQuoteHandling(UnescapedQuoteHandling.STOP_AT_DELIMITER)
    settings
  }
}

object CsvOptions {
  def extract(options: Map[String, String]): CsvOptions = {
    CsvOptions(
      delimiter = getChar("delimiter", options.get("delimiter"), ','),
      quote = getChar("quote", options.get("quote"), ','),
      escape = getChar("escape", options.get("escape"), ','),
      nullValue = options.getOrElse("nullValue", ""),
      nanValue = options.getOrElse("nanValue", "NaN"),
      positiveInf = options.getOrElse("positiveInf", "Inf"),
      negativeInf = options.getOrElse("negativeInf", "-Inf"),
      timestampFormat = options.getOrElse("timestampFormat", ""))
  }

  private def getChar(paramName: String, paramValue: Option[String], default: Char): Char = {
    paramValue match {
      case None => default
      case Some(null) => default
      case Some(value) if value.length == 0 => '\u0000'
      case Some(value) if value.length == 1 => value.charAt(0)
      case _ => throw new RuntimeException(s"$paramName cannot be more than one character")
    }
  }

}