package fi.pelam.csv

import org.junit.Assert._
import org.junit._

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
    assertEquals(Some("1"), SortedBiMap(1 -> "foo", 2 -> "bar").reverse.get("foo").map(_.map(_.toString).reduce(_ + "," + _)))
  }

  @Test
  def testReverseGet2() = {
    assertEquals(Some("1,2"), SortedBiMap(1 -> "foo", 2 -> "foo").reverse.get("foo").map(_.map(_.toString).reduce(_ + "," + _)))
  }

  @Test
  def testReverseGetNone() = {
    assertEquals(None, SortedBiMap(1 -> "foo", 2 -> "bar").reverse.get("baz"))
  }

  def assertReverseMapContents[K, V](expected: String, actual: SortedBiMap[K, V]): Unit = {

    val reverseMapElements: TraversableOnce[String] = actual.reverse.map {
      case (k, v: IndexedSeq[K]) => k + "->" + v.map(_.toString).reduce(_ + "," + _)
    }

    assertEquals(expected, reverseMapElements.reduce(_ + "\n" + _))
  }


}