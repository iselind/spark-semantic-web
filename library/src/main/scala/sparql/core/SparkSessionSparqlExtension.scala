package sparql.core

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.graphframes.GraphFrame
import sparql.core.context.SparQLContext

object SparkSessionSparqlExtension {
  implicit class SparqlEnhancedSession(spark: SparkSession)(implicit
      sparqlContext: SparQLContext
  ) {
    def sparql(query: String): DataFrame = {
      sparqlContext.execStrategy.execute(query)(spark, sparqlContext)
    }

    def registerGraph(frame: GraphFrame, name: String): Unit = {
      sparqlContext.graphStore.putGraph(name, frame)
    }
  }
}
