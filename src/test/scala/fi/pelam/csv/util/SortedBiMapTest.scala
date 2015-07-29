package fi.pelam.csv.util

import org.junit.Assert._
import org.junit._

import scala.collection.SortedMap

class SortedBiMapTest {

  @Test
  def testReverseSimple() = {
    assertReverseMapContents("foo->1,2", SortedBiMap(1 -> "foo", 2 -> "foo"))
  }

  @Test
  def testReverseSimpleOrdering() = {
    assertReverseMapContents("foo->1,2", SortedBiMap(2 -> "foo", 1 -> "foo"))
  }

  @Test
  def testReverseSimpleValueOrder() = {
    assertReverseMapContents("foo->1\nbar->2", SortedBiMap(1 -> "foo", 2 -> "bar"))
  }

  @Test
  def testReverseSimpleValueOrder2() = {
    assertReverseMapContents("foo->1\nbar->2", SortedBiMap(2 -> "bar", 1 -> "foo"))
  }

  @Test
  def testReverseSimpleValueOrder3() = {
    assertReverseMapContents("foo->1,3\nbar->2", SortedBiMap(1 -> "foo", 2 -> "bar", 3->"foo"))
  }

  @Test
  def testReverseSimpleValueOrder4() = {
    assertReverseMapContents("bar->1,3\nfoo->2", SortedBiMap(1 -> "bar", 2 -> "foo", 3->"bar"))
  }

  @Test
  def testGet() = {
    assertEquals(Some("foo"), SortedBiMap(1 -> "foo", 2 -> "bar").get(1))
    assertEquals(Some("bar"), SortedBiMap(1 -> "foo", 2 -> "bar").get(2))
    assertEquals(None, SortedBiMap(1 -> "foo", 2 -> "foo").get(3))
  }

  @Test
  def testReverseGet() = {
    assertOptionSeqContent(Some("1"), SortedBiMap(1 -> "foo", 2 -> "bar").reverse.get("foo"))
  }

  @Test
  def testReverseGet2() = {
    assertOptionSeqContent(Some("1,2"), SortedBiMap(1 -> "foo", 2 -> "foo").reverse.get("foo"))
  }

  @Test
  def testReverseGetNone() = {
    assertEquals(None, SortedBiMap(1 -> "foo", 2 -> "bar").reverse.get("baz"))
  }

  @Test
  def testSortedMapDelegation() = {
    assertKeys("2,3", SortedBiMap(1 -> "foo", 2 -> "bar", 3 -> "baz").range(2, 4))
  }

  @Test
  def testReverseSortedMapDelegation() = {
    assertKeys("bar,baz", SortedBiMap(1 -> "foo", 2 -> "bar", 3 -> "baz").reverse.range("bar", "zzz"))
  }

  def assertOptionSeqContent(expected: Option[String], s: Option[Seq[_]]) = {
    assertEquals(expected, s.map(_.map(_.toString).reduce(_ + "," + _)))
  }

  def assertKeys(expected: String, map: SortedMap[_, _]) = {
    assertEquals(expected, map.keys.map(_.toString).reduce(_ + "," + _))
  }

  def assertReverseMapContents[K, V](expected: String, actual: SortedBiMap[K, V]): Unit = {

    val reverseMapElements: TraversableOnce[String] = actual.reverse.map {
      case (k, v: IndexedSeq[K]) => k + "->" + v.map(_.toString).reduce(_ + "," + _)
    }

    assertEquals(expected, reverseMapElements.reduce(_ + "\n" + _))
  }
}