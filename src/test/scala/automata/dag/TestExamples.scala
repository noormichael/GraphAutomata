package automata.dag
import org.junit.Test
import org.junit.Assert.assertTrue
import scala.collection.immutable.MultiSet._
import scala.collection.immutable.Bag //suprisingly important to be explicit about this
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import automata.tree.TreeDfa
import scalax.collection.edge.Implicits._
import scalax.collection.GraphPredef._
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.GraphPredef
import automata.tree.LearnTreeAutomata
import org.scalatest.junit.AssertionsForJUnit

@Test
class TestExamples extends AssertionsForJUnit {

  /** allows us to concisely label graph nodes with prefixes */
  def describe(s: String) = s.split('_').head

  @Test
  def learnTrivalString: Unit = {

    //TODO need to figure out hyper edges to make these graph literals more concise
    val g = Graph(
      "a_0_0" ~> "a_0_1",

      "a_1_0" ~> "a_1_1", "a_1_1" ~> "a_1_2",

      "a_2_0" ~> "a_2_1", "a_2_1" ~> "a_2_2", "a_2_2" ~> "a_2_3")
    //    println(g)

    val detdag = LearnDeterministicDag.greedyLearn(g)(describe)
    //    println(detdag)

    assert(detdag.parse(g)(describe).isDefined, "should be able to parse itself")

    val g2 = Graph("a_3_0" ~> "a_3_1", "a_3_1" ~> "a_3_2", "a_3_2" ~> "a_3_3", "a_3_3" ~> "a_3_4")
    assert(detdag.parse(g2)(describe).isDefined, "should be able to parse a similar but unseen string")

  }

  @Test
  def learnConcretePattern: Unit = {

    //TODO need to figure out hyper edges to make these graph literals more concise
    val g = Graph(
      "a_0" ~> "b_0", "b_0" ~> "c_0", "c_0" ~> "d_0",

      "a_1" ~> "b_1", "b_1" ~> "c_1", "c_1" ~> "d_1",

      "a_2" ~> "b_2", "b_2" ~> "c_2", "c_2" ~> "d_2",

      "a_3" ~> "b_3", "b_3" ~> "c_3", "c_3" ~> "d_3",

      "a_4" ~> "b_4", "b_4" ~> "c_4", "c_4" ~> "d_4")
    //    println(g)

    val detdag = LearnDeterministicDag.greedyLearn(g)(describe)
    //TODO: when the code settles down we can make sure this converges to the expected litteral
    //    println
    //    println(detdag)

    assert(detdag.parse(g)(describe).isDefined, "should be able to parse itself")

    val g2 = Graph("a" ~> "b", "b" ~> "c", "c" ~> "d")
    assert(detdag.parse(g2)(describe).isDefined, "should be able to parse a similar but unseen string")

    val bad1 = Graph("a" ~> "b", "b" ~> "c", "c" ~> "a_1")

    //    println(detdag.parse(bad1)(describe))

    assert(!detdag.parse(bad1)(describe).isDefined, "should NOT be able to parse a string with a different patern")
  }

  //TODO: some of these may need the learning code to be more sensitive to work
  //TODO: repeated string pattern
  //TODO: multiple disjoint concrete strings

  //TODO: branching trees
  //TODO: concrete trees
  //TODO: branching trees with some implicit associations (all the leaves have to be consistant)

  //TODO: repeated dimonds
  //TODO: concrete dags
  //TODO: branching dags
  //TODO: partly repeating dags
  //TODO: the 3 sat grammar for fun
}