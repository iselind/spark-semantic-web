package sparql.core.ext

trait Node {
  def isVariable: Boolean
  def isURI: Boolean
  def isLiteral: Boolean

  def isBlank: Boolean

  def getBlankNodeLabel: String
  def getName: String
  def getURI: String
  def getLiteralLexicalForm: String

  def toString: String
}
