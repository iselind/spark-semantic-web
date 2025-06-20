package sparql.core.executionstrategy

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SparkSession
import sparql.core.QueryNode
import sparql.core.SparqlExecutionStrategy
import sparql.core.ext.SparqlParser

class SparkOnlyStrategy(parser: SparqlParser) extends SparqlExecutionStrategy {
  override def execute(query: String)(implicit
      spark: SparkSession
  ): DataFrame = {
    val ast: QueryNode = parser.parse(query)
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
