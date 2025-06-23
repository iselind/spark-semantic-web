package sparql.core.ext

import sparql.core.query.Executor

trait SparqlParser {
  def parse[T](query: String): Executor[T]
}
