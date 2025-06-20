package sparql.core.executionstrategy

import org.apache.spark.sql.{DataFrame, SparkSession}
import sparql.core.{QueryNode, SparqlExecutionStrategy, SparqlParser}

object SparkOnlyStrategy extends SparqlExecutionStrategy {
  override def execute(query: String)(implicit
      spark: SparkSession
  ): DataFrame = {
    val ast = SparqlParser.parse(query)
    if (ast.requiresFallback)
      throw UnsupportedQuery()
    if (ast.aborted)
      throw ParsingAborted(ast.abortReason)

    execute(ast)
  }

  def execute(ast: QueryNode)(implicit
      spark: SparkSession
  ): DataFrame = {
    // TODO: Convert the AST to actual Spark stuff
    ???
  }
}
