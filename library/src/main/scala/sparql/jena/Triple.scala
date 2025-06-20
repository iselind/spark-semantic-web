package sparql.jena

import org.apache.jena.sparql.core.TriplePath
import sparql.core.ext.Node

case class Triple(s: Node, p: Node, o: Node) extends sparql.core.ext.Triple {
  override def getSubject: Node = s

  override def getPredicate: Node = p

  override def getObject: Node = s

  override def getMathSubject: Node = ???

  override def getMathPredicate: Node = ???

  override def getMathObject: Node = ???
}

object Triple {
  def create(s: Node, p: Node, o: Node): Triple = {
    new Triple(s, p, o)
  }
  def from(src: TriplePath): Triple = ???
}
