package sparql.core.executionstrategy

import org.apache.spark.sql.SparkSession
import sparql.core.SparqlExecutionStrategy
import sparql.core.SparqlResult
import sparql.core.context.SparQLContext
import sparql.core.exception.UnsupportedQuery
import sparql.core.ext.Ask
import sparql.core.ext.Construct
import sparql.core.ext.ParsedQuery
import sparql.core.ext.Select
import sparql.core.ext.Unsupported

class SparkOnlyStrategy extends SparqlExecutionStrategy {
  override def execute(
      query: String
  )(implicit spark: SparkSession, sparqlContext: SparQLContext): SparqlResult = {
    val p: ParsedQuery = sparqlContext.sparqlParser.parse(query)
    p match {
      case a: Ask => execute(a)
      case c: Construct => execute(c)
      case s: Select => execute(s)
      case Unsupported(q) => throw new UnsupportedQuery(q)
    }
  }

  def execute(s: Select)(implicit spark: SparkSession, sparqlContext: SparQLContext): SparqlResult = ???
  def execute(a: Ask)(implicit spark: SparkSession, sparqlContext: SparQLContext): SparqlResult = ???
  def execute(c: Construct)(implicit spark: SparkSession, sparqlContext: SparQLContext): SparqlResult = ???
}
