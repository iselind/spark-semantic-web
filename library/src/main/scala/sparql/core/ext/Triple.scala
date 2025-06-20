package sparql.core.ext

trait Triple {
  def getSubject: Node
  def getPredicate: Node
  def getObject: Node

  def getMathSubject: Node
  def getMathPredicate: Node
  def getMathObject: Node
}
