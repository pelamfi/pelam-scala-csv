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

import scala.collection.generic.CanBuildFrom
import scala.collection.{BuildFrom, GenTraversableOnce, SortedMap, mutable}

/**
 * Bidirectional map supporting multiple values for a single key.
 *
 * This class is used in [[fi.pelam.csv.table.Table Table]] to map between
 * rows and columns and their types.
 *
 * The iteration order is determined by the keyOrdering implicit.
 *
 * The fact that the map is ordered by keys is useful in [[fi.pelam.csv.table.Table Table]]
 * because then the columns and rows will be ordered naturally in both the
 * forward and reverse maps.
 *
 * The ordering of values in reverse map is defined by the key order taking into account
 * the first key on which each value occurs.
 *
 * @note This class uses Scala's SortedMap under the hood.
 */
final case class SortedBiMap[K, V](map: SortedMap[K, V])(implicit keyOrdering: Ordering[K]) extends SortedMap[K, V] {

  import SortedBiMap._

  lazy val reverse: SortedMap[V, IndexedSeq[K]] = reverseMap(map)

  override def +[B1 >: V](kv: (K, B1)): SortedBiMap[K, B1] = SortedBiMap[K, B1](map + kv)

  override def drop(n: Int): SortedBiMap[K, V] = SortedBiMap(super.drop(n))

  override def take(n: Int): SortedBiMap[K, V] = SortedBiMap(super.take(n))

  //override def ++[B1 >: V](xs: GenTraversableOnce[(K, B1)]): SortedBiMap[K, B1] = SortedBiMap(super.++(xs))

  override def get(key: K): Option[V] = map.get(key)

  override def iterator: Iterator[(K, V)] = map.iterator

  override def -(key: K): SortedBiMap[K, V] = SortedBiMap[K, V](map - key)

  override def filter(p: ((K, V)) => Boolean): SortedBiMap[K, V] = SortedBiMap[K, V](map.filter(p))

  //override def filterKeys(p: K => Boolean): SortedBiMap[K, V] = SortedBiMap[K, V](map.filterKeys(p))

  override def ordering: Ordering[K] = keyOrdering

  override def valuesIteratorFrom(start: K): Iterator[V] = map.valuesIteratorFrom(start)

  override def rangeImpl(from: Option[K], until: Option[K]): SortedMap[K, V] = map.rangeImpl(from, until)

  override def iteratorFrom(start: K): Iterator[(K, V)] = map.iteratorFrom(start)

  override def keysIteratorFrom(start: K): Iterator[K] = map.keysIteratorFrom(start)

  override def -(key: K) = ???

  override def -(key1: K, key2: K, keys: K*) = ???
}

object SortedBiMap {

  /**
   * Constructor signature modeled after the one in
   * [[http://www.scala-lang.org/api/current/index.html#scala.collection.generic.SortedMapFactory SortedMapFactory]].
   */
  def apply[K, V](elements: (K, V)*)(implicit ordering: Ordering[K]): SortedBiMap[K, V] =
    SortedBiMap(SortedMap[K, V]() ++ elements)

  /**
   * Utility method for reversing a map. Multiple equal values with different keys
   * are handled by having IndexedSeq with values as the value in the resulting map.
   *
   * Almost the same as the code suggested in StackOverflow post
   * [[http://stackoverflow.com/a/24222250/1148030 StackOverflow]] which would be:
   * {{{
   *   map.groupBy(_._2).mapValues(_.map(_._1).toIndexedSeq
   * }}}
   *
   * However this is a hand rolled implementation. GroupBy uses uses HashMap internally for values, but I
   * want to keep the order provided by the keys. Also I'm a bit wary of the implications of
   * this [[http://stackoverflow.com/questions/14882642/scala-why-mapvalues-produces-a-view-and-is-there-any-stable-alternatives
   * issue discussed on StackOverflow]].
   *
   * The implementation is complicated and probably not very efficent efficient because Scala
   * does not provide [[https://lauris.github.io/map-order-scala/ immutable map with insertion order as iteration order]].
   * Also due to this the result is a SortedMap with a custom Ordering.
   */
  private[csv] def reverseMap[K, V](map: SortedMap[K, V])(implicit keyOrdering: Ordering[K]): SortedMap[V, IndexedSeq[K]] = {

    val valueMap = mutable.HashMap.empty[V, mutable.Builder[K, IndexedSeq[K]]]

    val valueToFirstKey = mutable.HashMap.empty[V, K]

    // Order of values in result is determined by the order of first occurences of each value
    // TODO: Is there some efficient implementation of immutable map with insertion order as iteration order somewhere
    for ((key, value) <- map; if !valueToFirstKey.contains(value)) {
      valueToFirstKey.put(value, key)
    }

    object ValueOrder extends Ordering[V] {
      def compare(a: V, b: V): Int = {
        (valueToFirstKey.get(a), valueToFirstKey.get(b)) match {
          case (Some(a), Some(b)) => keyOrdering.compare(a, b)
          case (None, Some(b)) => 1
          case (Some(a), None) => -1
          case (None, None) => 0
        }
      }
    }

    for ((key, value) <- map) {
      val keysBuilder = valueMap.getOrElseUpdate(value, IndexedSeq.newBuilder)
      keysBuilder += key
    }

    val resultBuilder = SortedMap.newBuilder[V, IndexedSeq[K]](ValueOrder)

    for ((value, keysBuilder) <- valueMap) {
      val pair = (value, keysBuilder.result)
      resultBuilder += pair
    }

    resultBuilder.result()
  }

  def empty[A, B](implicit ord: Ordering[A]): SortedBiMap[A, B] = SortedBiMap()

  class BiMapBuilder[A, B](val ord: Ordering[A]) extends mutable.Builder[(A, B), SortedBiMap[A, B]] {
    val pairs = mutable.Buffer[(A, B)]()

    override def addOne(elem: (A, B)): BiMapBuilder.this.type = {
      pairs += elem
      this
    }

    override def result(): SortedBiMap[A, B] = {
      SortedBiMap(SortedMap.empty(ord) ++ pairs)(ord)
    }

    override def clear(): Unit = {
      pairs.clear()
    }
  }

  // TODO: Is this needed (implicit ord: Ordering[A])
  class BiMapBuildFrom[A, B] extends BuildFrom[SortedBiMap[A, B], (A, B), SortedBiMap[A, B]] {

    override def fromSpecific(from: SortedBiMap[A, B])(pairs: IterableOnce[(A, B)]) = {
      SortedBiMap(SortedMap.empty(from.ordering) ++ pairs)(from.ordering)
    }

    override def newBuilder(from: SortedBiMap[A, B]) = new BiMapBuilder[A, B](from.ordering)
  }

  implicit def buildFrom[A, B](implicit ord: Ordering[A]): BuildFrom[SortedBiMap[A, B], (A, B), SortedBiMap[A, B]] =
    new BiMapBuildFrom[A, B]
}
