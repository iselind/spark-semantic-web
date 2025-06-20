package sparql.core.ext

trait Triple {
  def getSubject: Node
  def getPredicate: Node
  def getObject: Node

  def getMatchSubject: Node

  def getMatchPredicate: Node

  def getMatchObject: Node
}
