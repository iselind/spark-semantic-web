package sparql.core

import org.apache.spark.sql.SparkSession
import sparql.core.context.SparQLContext

trait SparqlExecutionStrategy {
  def execute(
      query: String
  )(implicit spark: SparkSession, sparqlContext: SparQLContext): SparqlResult
}
