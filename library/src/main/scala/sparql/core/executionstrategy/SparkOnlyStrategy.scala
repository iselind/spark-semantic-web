package sparql.core.executionstrategy

import org.apache.spark.sql.{DataFrame, SparkSession}
import sparql.core.SparqlExecutionStrategy
import sparql.core.context.SparQLContext
import sparql.core.exception.{ParsingAborted, UnsupportedQuery}
import sparql.core.ext.SparqlParser
import sparql.core.query.QueryNode

class SparkOnlyStrategy(parser: SparqlParser) extends SparqlExecutionStrategy {
  override def execute(
      query: String
  )(implicit spark: SparkSession, sparqlContext: SparQLContext): DataFrame = {
    val qn: QueryNode = parser.parse(query)
    val ast = qn.where
    if (ast.requiresFallback)
      throw UnsupportedQuery()
    if (ast.aborted)
      throw ParsingAborted(ast.abortReason)

    execute(qn)
  }

  def execute(ast: QueryNode)(implicit
      spark: SparkSession
  ): DataFrame = {
    // TODO: Convert the AST to actual Spark stuff
    ???
  }
}
