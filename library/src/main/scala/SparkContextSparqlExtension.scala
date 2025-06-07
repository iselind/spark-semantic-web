import org.apache.spark.sql.SparkSession

// Adds a `sparql` method to SparkSession
object SparkSessionSparqlExtension {

  implicit class SparqlEnhancedSession(spark: SparkSession) {

    def sparql(query: String): Unit = {
      // Dummy implementation — replace with actual SPARQL logic
      println(s"Running SPARQL query: $query")
    }
  }
}
