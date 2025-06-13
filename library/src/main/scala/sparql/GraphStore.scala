package sparql

import org.graphframes.GraphFrame

trait GraphStore {

  /** Get a graph by name; error or Option if not found */
  def getGraph(name: String): Option[GraphFrame]

  /** Register or replace a named graph */
  def putGraph(name: String, graph: GraphFrame): Unit

  /** Remove a named graph */
  def removeGraph(name: String): Unit

  /** List all registered graph names */
  def listGraphs(): Seq[String]
}
