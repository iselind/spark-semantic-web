package sparql.core.executionstrategy

import org.apache.spark.sql.{DataFrame, SparkSession}
import sparql.core.SparkSessionSparqlExtension.SparqlEnhancedSession
import sparql.core.{FallbackHandler, SparqlExecutionStrategy}

/** Assumes the graph to use is registered as "Jena" in the spark session
  */
object JenaOnlyStrategy extends SparqlExecutionStrategy {

  /** Use Apache Jena to execute query
    *
    * @param query
    *   The query to perform
    * @return
    *   Apache Spark DataFrame
    */
  def execute(
      query: String
  )(implicit spark: SparkSession): DataFrame = {
    val graphStore = spark.graphStore
    if (graphStore.listGraphs().size != 1)
      throw IncompatibleGraphStore("Unexpected number of named graphs")

    val graph = graphStore.getGraph("Jena")
    graph match {
      case Some(graph) =>
        FallbackHandler.fallback(query, graph)
      case None =>
        throw NoSuchGraph("Jena")
    }
  }
}
