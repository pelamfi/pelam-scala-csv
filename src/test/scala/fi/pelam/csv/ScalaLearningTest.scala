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

package fi.pelam.csv

import org.junit.Test

trait Combinations[S] {
  def flatMap(inner: S => Combinations[S]): Combinations[S]
  def map(mapFunc: S => S): Combinations[S]
  def run(combination: Int): S

  def run(): Seq[S] = {
    var s = List[S]()
    for (i <- Seq(1,2,3)) {
      s = s :+ run(i)
    }
    s
  }
}

case class Flatmapped[S](items: IndexedSeq[S], inner: S => Combinations[S]) extends Combinations[S] {
  override def flatMap(inner: (S) => Combinations[S]): Combinations[S] = Flatmapped(items, inner)

  override def map(mapFunc: (S) => S): Combinations[S] = Mapped(this, mapFunc)

  override def run(combination: Int): S = {
    inner(items(combination % items.size)).run(combination / items.size)
  }
}

case class Mapped[S](outer: Combinations[S], mapFunc: S => S) extends Combinations[S] {
  override def flatMap(inner: (S) => Combinations[S]): Combinations[S] = ???

  override def map(mapFunc: (S) => S): Combinations[S] = Mapped(this, mapFunc)

  override def run(combination: Int): S = {
    mapFunc(outer.run(combination))
  }
}

case class Items[S](items: IndexedSeq[S]) extends Combinations[S] {
  override def flatMap(inner: S => Combinations[S]): Combinations[S] = {
    Flatmapped(items, inner)
  }

  override def map(mapFunc: S => S): Combinations[S] = {
    Mapped(this, mapFunc)
  }

  override def run(combination: Int): S = {
    items(combination)
  }
}

class ScalaLearningTest {

  @Test
  def testForCombinationGeneration(): Unit = {


    val r = for (x <- Items(IndexedSeq("a", "b"));
         y <- Items(IndexedSeq("c", "d"))) yield {
      x + y
    }

    println(r.run())
  }

}
