package sparql.jena

import org.apache.jena.graph

object NodeFactory {

  def createVariable(s: String): Node = new Node(
    graph.NodeFactory.createVariable(s)
  )

  def createURI(s: String): Node = new Node(graph.NodeFactory.createURI(s))

  def createLiteral(s: String): Node = new Node(
    graph.NodeFactory.createLiteral(s)
  )
}
