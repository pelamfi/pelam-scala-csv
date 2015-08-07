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

package fi.pelam.csv.stream

import java.io.StringReader

import fi.pelam.csv.cell.{CellKey, StringCell}
import org.junit.Assert._
import org.junit.Test


/**
 * @note [[fi.pelam.csv.stream.CsvReaderInternal CsvReaderInternal]]
 *       is mostly tested indirectly through its wrapper's
 *       tests in [[fi.pelam.csv.stream.CsvReaderTest CsvReaderTest]].
 */
class CsvReaderInternalTest {

  @Test
  def testRead: Unit = {
    val reader = new CsvReaderInternal(new StringReader("fooxbar\n"), 'x')
    assertEquals(Some(Right(StringCell(CellKey(0, 0), "foo"))), reader.read())
    assertEquals(Some(Right(StringCell(CellKey(0, 1), "bar"))), reader.read())
    assertEquals(None, reader.read())
    assertEquals("Subsequent calls give None", None, reader.read())
  }

  @Test
  def testReadEmpty: Unit = {
    val reader = new CsvReaderInternal(new StringReader(""), 'x')
    assertEquals(None, reader.read())
    assertEquals("Subsequent calls give None", None, reader.read())
  }

  @Test
  def testReadError: Unit = {
    val reader = new CsvReaderInternal(new StringReader("\"\n"), 'x')
    assertEquals(Some(Left(CsvReaderError("Unclosed quote", CellKey(0, 0)))), reader.read())
    assertEquals("Subsequent calls give None", None, reader.read())
  }
}