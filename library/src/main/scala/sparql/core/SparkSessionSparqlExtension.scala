package sparql.core

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SparkSession
import org.graphframes.GraphFrame
import sparql.core.context.SparQLContext

trait SparqlQueryExecutor {
  def select(query: String): DataFrame

  def construct(query: String): GraphFrame

  def ask(query: String): Boolean
}

object SparkSessionSparqlExtension {
  implicit class SparqlEnhancedSession(spark: SparkSession)(implicit sparqlContext: SparQLContext) {
    def sparql(query: String): SparqlResult = {
      sparqlContext.execStrategy.execute(query)(spark, sparqlContext)
    }

    def registerGraph(frame: GraphFrame, name: String): Unit = {
      sparqlContext.graphStore.putGraph(name, frame)
    }
  }

  implicit class SparqlEnhancedGraph(g: GraphFrame)(implicit spark: SparkSession,sparqlContext: SparQLContext) {
    def sparql(query: String): SparqlResult = {
      sparqlContext.execStrategy.execute(query)(spark, sparqlContext)
    }
  }
}
