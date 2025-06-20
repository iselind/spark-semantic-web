package sparql.jena

class Node(n: org.apache.jena.graph.Node) extends sparql.core.ext.Node {
  override def isVariable: Boolean = n.isVariable

  override def isURI: Boolean = n.isURI

  override def isLiteral: Boolean = n.isLiteral

  override def isBlank: Boolean = n.isBlank

  override def getBlankNodeLabel: String = n.getBlankNodeLabel

  override def getName: String = n.getName

  override def getURI: String = n.getURI

  override def getLiteralLexicalForm: String = n.getLiteralLexicalForm

  override def toString: String = n.toString
}

object Node extends {
  val ANY: Node = new Node(org.apache.jena.graph.Node.ANY)
}
