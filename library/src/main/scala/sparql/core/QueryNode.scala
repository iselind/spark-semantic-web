package sparql.core

import sparql.core.ext.FilterExpression
import sparql.core.ext.Triple

// Representation of recursive query structure with fallback tracking
case class QueryNode(
    bgp: List[Triple] = List(),
    filters: List[FilterExpression] = List(),
    unions: List[List[QueryNode]] = List(),
    optionals: List[QueryNode] = List(),
    others: List[String] = List(),
    aborted: Boolean = false,
    abortReason: Option[String] = None,
    requiresFallback: Boolean = false
)
