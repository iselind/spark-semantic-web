package sparql.executionstrategy

import org.apache.spark.sql.{DataFrame, SparkSession}
import sparql.{SparqlExecutionStrategy, SparqlParser}

object HybridFallbackStrategy extends SparqlExecutionStrategy {
  override def execute(query: String)(implicit
      spark: SparkSession
  ): DataFrame = {
    val ast = SparqlParser.parse(query)
    if (ast.requiresFallback || ast.aborted) {
      JenaOnlyStrategy.execute(query)
    } else {
      SparkOnlyStrategy.execute(ast)
    }
  }
}
