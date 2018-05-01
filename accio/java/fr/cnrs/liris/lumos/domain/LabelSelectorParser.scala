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

package fr.cnrs.liris.lumos.domain

import scala.util.parsing.combinator._
import scala.util.parsing.input.{NoPosition, Position, Reader}

private[domain] object LabelSelectorParser {

  sealed trait Token
  case class IDENTIFIER(str: String) extends Token
  case object NOT extends Token
  case object EQUALS extends Token
  case object DIFFERENT extends Token
  case object LPARENS extends Token
  case object RPARENS extends Token
  case object COMMA extends Token
  case object IN extends Token
  case object NOTIN extends Token

  object Lexer extends RegexParsers {
    override def skipWhitespace = true
    override val whiteSpace = "[ \t]+".r

    def identifier ="""[a-zA-Z0-9][a-zA-Z0-9._-]*""".r ^^ (s => IDENTIFIER(s) )
    def different = "!=" ^^ (_ => DIFFERENT)
    def equals = "=" ^^ (_ => EQUALS)
    def not = "!" ^^ (_ => NOT)
    def comma = "," ^^ (_ => COMMA)
    def lparens = "(" ^^ (_ => LPARENS)
    def rparens = ")" ^^ (_ => RPARENS)
    def in = "in" ^^ (_ => IN)
    def notin = "notin" ^^ (_ => NOTIN)

    def tokens: Parser[List[Token]] = {
      phrase(rep1(in | notin | identifier | different | equals | not | comma | lparens | rparens)) ^^ identity
    }

    def apply(str: String):  Either[String, List[Token]] = {
      parse(tokens, str) match {
        case NoSuccess(msg, _) => Left(msg)
        case Success(res, _) => Right(res)
      }
    }
  }

  object Parser extends Parsers {
    override type Elem = Token

    import LabelSelector.Req

    def sel = phrase(repsep(req, COMMA)) ^^ (reqs => LabelSelector(reqs))
    def req: Parser[Req] = equals | different | in | notin | absent| present
    def present = identifier ^^ { key => Req(key.str, LabelSelector.Present) }
    def absent = (NOT ~ identifier) ^^ { case _ ~ key => Req(key.str, LabelSelector.Absent) }
    def equals = (identifier ~ EQUALS ~ identifier) ^^ { case key ~ _ ~ value => Req(key.str, LabelSelector.In, Set(value.str)) }
    def different = (identifier ~ DIFFERENT ~ identifier) ^^ { case key ~ _ ~ value => Req(key.str, LabelSelector.NotIn, Set(value.str)) }
    def in = (identifier ~ IN ~ values) ^^ { case key ~ _ ~ values => Req(key.str, LabelSelector.In, values) }
    def notin = (identifier ~ NOTIN ~ values) ^^ { case key ~ _ ~ values => Req(key.str, LabelSelector.NotIn, values) }
    def values: Parser[Set[String]] = (LPARENS ~ repsep(identifier, COMMA) ~ RPARENS) ^^ { case _ ~ values ~ _ => values.map(_.str).toSet }

    def identifier: Parser[IDENTIFIER] = accept("identifier", { case key: IDENTIFIER => key })

    def apply(tokens: Seq[Token]): Either[String, LabelSelector] = {
      val reader = new TokenReader(tokens)
      sel(reader) match {
        case Success(res, _) => Right(res)
        case NoSuccess(msg, _) => Left(msg)
      }
    }
  }

  class TokenReader(tokens: Seq[Token]) extends Reader[Token] {
    override def first: Token = tokens.head
    override def atEnd: Boolean = tokens.isEmpty
    override def pos: Position = NoPosition
    override def rest: Reader[Token] = new TokenReader(tokens.tail)
  }

  def parse(str: String): Either[String, LabelSelector] = {
    for {
      tokens <- Lexer(str).right
      selector <- Parser(tokens).right
    } yield selector
  }
}
