/*
 * This file is part of pelam-scala-csv
 *
 * Copyright © Peter Lamberg 2015 (pelam-scala-csv@pelam.fi)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.pelam.csv.util

import java.text.{NumberFormat, ParseException, ParsePosition}
import java.util.Locale

object FormatterUtil {
  type Formatter[T] = (T) => String
  type Parser = (String) => Option[Number]

  /**
   * This function is a workaround for the Java feature of `NumberFormat`
   * not being thread safe. http://stackoverflow.com/a/1285353/1148030
   *
   * This transformation returns a thread safe `Formatter` based on the
   * `NumberFormat` passed in as parameter.
   */
  def toSynchronizedFormatter[T](numberFormat: NumberFormat): Formatter[T] = {

    // Clone to ensure instance is only used in this scope
    val clonedFormatter = numberFormat.clone().asInstanceOf[NumberFormat]

    val formatterFunction = { (input: T) =>
      clonedFormatter.synchronized {
        clonedFormatter.format(input)
      }
    }

    formatterFunction
  }

  /**
   * This function is a workaround for the Java feature of `NumberFormat`
   * not being thread safe. http://stackoverflow.com/a/1285353/1148030
   *
   * This transformation returns a thread safe `Parser` based on the
   * `NumberFormat` passed in as parameter.
   */
  def toSynchronizedParser(numberFormat: NumberFormat): Parser = {

    // Clone to ensure instance is only used in this scope
    val clonedFormatter = numberFormat.clone().asInstanceOf[NumberFormat]

    val parserFunction = { (input: String) =>

      val position = new ParsePosition(0)

      val number = clonedFormatter.synchronized {
        clonedFormatter.parse(input, position)
      }

      if (position.getIndex() == input.size) {
        Some(number)
      } else {
        None
      }

    }

    parserFunction
  }

}
