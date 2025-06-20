package sparql.core.ext

import sparql.core.QueryNode

trait SparqlParser {
  def parse(query: String): QueryNode
}
