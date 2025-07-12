package sparql.jena.executionstrategy

import sparql.core.SparqlDataFrame
import sparql.core.SparqlExecutionStrategy
import sparql.core.SparqlResult
import sparql.core.context.SparQLContext
import sparql.core.exception.NoSuchGraph
import sparql.jena.FallbackHandler
import org.apache.spark.sql.SparkSession

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
  )(implicit
      spark: SparkSession,
      sparqlContext: SparQLContext
  ): SparqlResult = {
    val graphStore = sparqlContext.graphStore
    if (graphStore.listGraphs().size != 1)
      throw IncompatibleGraphStore("Unexpected number of named graphs")

    val graph = graphStore.getGraph("Jena")
    graph match {
      case Some(graph) =>
        SparqlDataFrame(FallbackHandler.fallback(query, graph))
      case None =>
        throw NoSuchGraph("Jena")
    }
  }
}
