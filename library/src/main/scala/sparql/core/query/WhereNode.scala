package sparql.core.query

import org.graphframes.GraphFrame
import sparql.core.GraphResolver
import sparql.core.ext.FilterExpression
import sparql.core.ext.Triple

// Representation of recursive query structure with fallback tracking
case class WhereNode(
    bgp: List[Triple] = List(),
    filters: List[FilterExpression] = List(),
    unions: List[List[WhereNode]] = List(),
    optionals: List[WhereNode] = List(),
    others: List[String] = List(),
    requiresFallback: Boolean = false
                    ) extends Executor {
  override def run(aliasMap: Map[String, String], graphResolver: GraphResolver)(frame: Option[GraphFrame]): GraphFrame = ???
}
