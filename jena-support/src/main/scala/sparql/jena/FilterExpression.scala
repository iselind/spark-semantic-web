package sparql.jena

import org.apache.jena.sparql.expr.Expr

import scala.annotation.unused

class FilterExpression extends sparql.core.ext.FilterExpression {}
object FilterExpression {
  def from(@unused e: Expr): FilterExpression = new FilterExpression
}
