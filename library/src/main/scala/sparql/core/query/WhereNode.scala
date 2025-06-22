package sparql.core.query

import sparql.core.ext.{FilterExpression, Triple}

// Representation of recursive query structure with fallback tracking
case class WhereNode(
    bgp: List[Triple] = List(),
    filters: List[FilterExpression] = List(),
    unions: List[List[WhereNode]] = List(),
    optionals: List[WhereNode] = List(),
    others: List[String] = List(),
    aborted: Boolean = false,
    abortReason: Option[String] = None,
    requiresFallback: Boolean = false
)
