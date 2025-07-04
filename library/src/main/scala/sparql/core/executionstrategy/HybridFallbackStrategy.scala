package sparql.core.executionstrategy

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.zookeeper.KeeperException.BadArgumentsException
import sparql.core.SparqlExecutionStrategy
import sparql.core.context.SparQLContext
import sparql.core.ext.SparqlParser

class HybridFallbackStrategy(
    fallback: SparqlExecutionStrategy,
    parser: SparqlParser
) extends SparqlExecutionStrategy {
  private val sparqlParser = new SparkOnlyStrategy(parser)

  if (fallback.isInstanceOf[SparkOnlyStrategy]) {
    throw new BadArgumentsException(
      "Doesn't make sense to use SparkOnlyStrategy as fallback"
    )
  }

  override def execute(
      query: String
  )(implicit spark: SparkSession, sparqlContext: SparQLContext): DataFrame = {
    val qn = parser.parse(query)
    val ast = qn.where
    if (ast.requiresFallback || ast.aborted) {
      fallback.execute(query)
    } else {
      sparqlParser.execute(qn)
    }
  }
}
