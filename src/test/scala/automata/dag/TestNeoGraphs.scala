package automata.dag
import java.io._
import neo4j_scala_graph.NeoData._
import org.neo4j.driver.v1.{ AuthTokens, GraphDatabase }
import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit
import automata.tree.TreeAutomata._
import scalax.collection.Graph
import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._
import scalax.collection.edge.LDiEdge // labeled directed edge
import scalax.collection.edge.Implicits._ // shortcuts
import org.neo4j.driver.v1.StatementResult

import org.junit.Ignore

object TestNeoGraphs {
  sealed trait Desc
  case class Artifact(name: String) extends Desc
  case class Process(name: String) extends Desc
  case class Agent(name: String) extends Desc
  case class Generic(name: String) extends Desc
  case class EdgeDesc(t: String) extends Desc

}

class TestNeoGraphs extends AssertionsForJUnit {

  import TestNeoGraphs._
  
  def writeGraphInitial(g: Graph[NeoData, DiEdge]) = {
    val fos = new FileOutputStream("initialgraph.obj")
    val oos = new ObjectOutputStream(fos)
    oos.writeObject(g)
    oos.close
  }

  def writeGraphFinal(g: Graph[NeoData, DiEdge]) = {
    val fos = new FileOutputStream("finalgraph.obj")
    val oos = new ObjectOutputStream(fos)
    oos.writeObject(g)
    oos.close
  }

  def createGraphFromDag(dagdfa: DagDfaFast[_]): Graph[NeoData, DiEdge] = {
    val list_trans = dagdfa.inputTree.transitions
    println(list_trans)
    var idtoLabel = Map[Int, Set[String]]()
    var g = Graph[NeoData, DiEdge]()
    for (li <- list_trans) {
      if (li.label.isInstanceOf[Artifact]) {
        idtoLabel += li.to -> Set("Artifact")
      }
      if (li.label.isInstanceOf[Process]) {
        idtoLabel += li.to -> Set("Process")
      }
    }
    var nodes = Set[NeoData]()
    var edges = Set[DiEdge[NeoNode]]()
    for (item <- list_trans) {
      var node = NeoNode(item.to, idtoLabel(item.to), Map())
      // var node = NeoNode(item.to, Set("???"), Map())
      nodes += node
      for (ances <- item.from) {
        var node_2 = NeoNode(ances, idtoLabel(item.to), Map())
        nodes += node_2
        edges += node_2 ~> node
      }
    }
    for (edge <- edges) {
      g += edge
    }
    return g
  }

  def describe(nd: NeoData): Desc = nd match {
    case NeoNode(_, labels, prop) if labels == Set("Artifact") => {
      val path = prop.getOrElse("path", "").asInstanceOf[String]
      if (path.startsWith("/usr/lib")) {
        return Artifact("/usr/lib/")
      } else if (path.startsWith("/etc/")) {
        return Artifact("/etc/")
      } else if (path.startsWith("/home/")) {
        return Artifact("/home/")
      } else if (path.startsWith("/usr/bin")) {
        return Artifact("/usr/bin/")
      } else if (path.startsWith("/usr/share")) {
        return Artifact("/usr/share/")
      } else if (path.startsWith("/dev/")) {
        return Artifact("/dev/")
      }
      val source_addr = prop.getOrElse("source address", "").asInstanceOf[String]
      if (source_addr.length() > 1) {
        return Artifact(source_addr)
      }
      val dest_addr = prop.getOrElse("destination address", "").asInstanceOf[String]
      if (dest_addr.length() > 1) {
        return Artifact(dest_addr)
      }
      return Artifact("")
    }
    case NeoNode(_, labels, prop) if labels == Set("Process") => {
      Process(prop.getOrElse("name", "???").asInstanceOf[String])
    }
    case NeoNode(_, labels, prop) if labels == Set("Agent") => {
      Agent("")
    }
    case NeoNode(_, labels, prop) if labels == Set() => {
      Generic("")
    }
    case NeoRel(_, t, _) => EdgeDesc(t)

  }

  def describe_original(nd: NeoData): Desc = nd match { // [!!!] Windows Graph fails here, Matching done incorrectly
    case NeoNode(_, labels, prop) if labels == Set("Artifact") => Artifact(prop.getOrElse("path", "???").asInstanceOf[String])
    case NeoNode(_, labels, prop) if labels == Set("Process") => Process(prop.getOrElse("name", "???").asInstanceOf[String]) //TODO: Option
    case NeoNode(_, labels, prop) if labels == Set("Agent") => Agent("")
    case NeoNode(_, labels, prop) if labels == Set() => Generic("")
    case NeoRel(_, t, _) => /*println(t);*/ EdgeDesc(t)
  }

  @Ignore
  @Test
  def learnNeo4jOnlyActivities: Unit = {
    val driver = GraphDatabase.driver("bolt://localhost", AuthTokens.basic("neo4j", "oldnew"))
    val session = driver.session();
    val g = toDiGraph(run(session)("MATCH (n)-[r]-()  where n.type='Process' AND r.type='WasTriggeredBy' RETURN n,r;"))
    println(g)
    println("result")
    println(LearnDeterministicDag.greedyLearn(g, 10)(describe, describe_original))
  }

  def dagGraph[A](g: Graph[A, DiEdge]): Graph[A, DiEdge] = {

    val ns = scala.collection.mutable.Set[A]()
    val es = scala.collection.mutable.Set[DiEdge[A]]()

    for (n <- g.nodes){
      ns.add(n)

      for(g.EdgeT(in, out) <- g.edges){   // how to only get out-edges of node??
        if((in == n) && !ns.contains(out)){
          es.add((in.value ~> out.value))
        }
      }
    }

    return Graph.from(g.nodes, es.toSet)
  }

  // bigger graphs, may need to give the jvm needs more memory
  @Test
  def learnNeo4j: Unit = {
    {
      val mb = 1024 * 1024;
      val run = Runtime.getRuntime();
      println(run.totalMemory() / mb)
    }
    // I REALLY hate how the prov arrows go in the opposite direction of cuasality, unlike literally everything ever.  who thought this was a good idea?
    val driver = GraphDatabase.driver("bolt://localhost", AuthTokens.basic("neo4j", "oldnew"))
    val session = driver.session();
    val g: Graph[NeoData, DiEdge] = fullGraph(session)
    println("result")

    // create DAG out of g to remove cycles
    // remove all back edges in dfs traversal of g

    val ng = dagGraph(g)

    val dfa = LearnDeterministicDag.greedyLearn(ng, 30)(describe, describe_original)
    println(dfa)
    LearnDeterministicDag.writeGrammar(dfa)
  }

  @Test
  def learnNeo4jAugmented: Unit = {
    {
      val mb = 1024 * 1024;
      val run = Runtime.getRuntime();
      println(run.totalMemory() / mb)
    }
    val prev_grammar = LearnDeterministicDag.readGrammar[Desc]()
    println("reading done")
    val driver = GraphDatabase.driver("bolt://localhost", AuthTokens.basic("neo4j", "oldnew"))
    val session = driver.session();
    val g = fullGraph(session)
    val middleGrammar = LearnDeterministicDag.augmentGrammar(prev_grammar, g)(describe,describe_original)
    println(middleGrammar)
  }
}
