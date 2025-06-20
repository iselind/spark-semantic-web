package sparql.jena

import org.apache.jena.sparql.core.TriplePath
import sparql.core.ext

case class Triple(s: Node, p: Node, o: Node) extends sparql.core.ext.Triple {
  override def getSubject: Node = s

  override def getPredicate: Node = p

  override def getObject: Node = o

  override def getMatchSubject: ext.Node = s

  override def getMatchPredicate: ext.Node = p

  override def getMatchObject: ext.Node = o
}

object Triple {
  val ANY: Triple = new Triple(Node.ANY, Node.ANY, Node.ANY)

  def create(s: Node, p: Node, o: Node): Triple = {
    if (p == Node.ANY && s == Node.ANY && o == Node.ANY)
      return ANY
    new Triple(s, p, o)
  }

  def from(src: TriplePath): Triple = {
    val t = src.asTriple()
    create(
      new Node(t.getSubject),
      new Node(t.getPredicate),
      new Node(t.getObject)
    )
  }
}
