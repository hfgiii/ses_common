package org.hfgiii.ses.common.csv.parser


import java.text.SimpleDateFormat

import org.parboiled2.{CharPredicate, StringBuilding, Parser}

/*
 * Copyright (C) 2014 Juergen Pfundt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This version differs from the original by abstracting over the result type of each line parse..
 * Generating desired output is achieved by implementing with the following method:
 *    {{
 *        def genOutput:(Seq[String]) => O
 *    }}
 *
 * @tparam O - The result type of each line parse
 */

trait CSVParsboiledParsers {

  trait CSVParboiledParserSB[O] extends Parser with StringBuilding {
    val formatter = new SimpleDateFormat("yyyy-MM-dd")

    /* start of csv parser */
    def csvfile = rule {
      (hdr ~ zeroOrMore(row)) ~> makeListOfOutputValues ~ zeroOrMore(optional("\r") ~ "\n") ~ EOI
    }

    def hdr = rule {
      row
    }

    def row = rule {
      oneOrMore(field).separatedBy(",") ~> genOutput ~ optional("\r") ~ "\n"
    }

    def field = rule {
      string | text | MATCH ~> makeEmpty
    }

    def text = rule {
      clearSB() ~ oneOrMore(noneOf(",\"\n\r") ~ appendSB()) ~ push(sb.toString) ~> makeText
    }

    def string = rule {
      WS ~ "\"" ~ clearSB() ~ zeroOrMore(("\"\"" | noneOf("\"")) ~ appendSB()) ~ push(sb.toString) ~> makeString ~ "\"" ~ WS
    }

    val whitespace = CharPredicate(" \t")

    def WS = rule {
      zeroOrMore(whitespace)
    }

    def genOutput: (Seq[String]) => O

    def makeListOfOutputValues = (h: O, r: Seq[O]) => h :: (r.toList: List[O])

    /* parser action */
    def makeText: (String) => String

    def makeString: (String) => String

    def makeEmpty: () => String

    def name: String
  }

  trait CSVParserAction {
    // remove leading and trailing blanks
    def makeText = (text: String) => text.trim

    // replace sequence of two double quotes by a single double quote
    def makeString = (string: String) => string.replaceAll("\"\"", "\"")

    // modify result of EMPTY token if required
    def makeEmpty = () => ""
  }

  trait CSVParserIETFAction extends CSVParserAction {
    // no trimming of WhiteSpace
    override def makeText = (text: String) => text
  }

}

object CSVParsboiledParsers extends CSVParsboiledParsers