package sparql.jena

import org.apache.jena.sparql.expr.Expr

class FilterExpression extends sparql.core.ext.FilterExpression {}
object FilterExpression {
  def from(e: Expr): FilterExpression = {
    new FilterExpression
  }
}
