package sparql.core.query

import sparql.core.ext.Node

case class SelectNode(vars: List[String], uris: List[Node])
