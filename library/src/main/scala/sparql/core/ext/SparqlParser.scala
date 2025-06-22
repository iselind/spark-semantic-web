package sparql.core.ext

import sparql.core.query.QueryNode

trait SparqlParser {
  def parse(query: String): QueryNode
}
