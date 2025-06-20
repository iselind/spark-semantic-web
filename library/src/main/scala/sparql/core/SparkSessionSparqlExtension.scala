package sparql.core

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.graphframes.GraphFrame
import sparql.core.graphstore.MapGraphStore

import scala.collection.concurrent.TrieMap

object SparkSessionSparqlExtension {
  /*
  This is needed because of how Spark works with implicits.

  Each time an implicit is accessed Spark will create a new wrapper.
  We're using a thread-safe map here to keep one graph store per Spark session and still enable concurrent access to it.
   */
  private val sessionToStore = TrieMap[SparkSession, GraphStore]()

  implicit class SparqlEnhancedSession(spark: SparkSession) {

    /** Lazily initialized store of registered graphs for this SparkSession.
      * Intended for advanced and internal usage — prefer `registerGraph` for
      * most use cases.
      */
    def graphStore: GraphStore =
      sessionToStore.getOrElseUpdate(spark, new MapGraphStore())

    def sparql(
        query: String
    )(implicit execStrategy: SparqlExecutionStrategy): DataFrame = {
      execStrategy.execute(query)(spark)
    }

    def registerGraph(frame: GraphFrame, name: String): Unit = {
      graphStore.putGraph(name, frame)
    }
  }
}
