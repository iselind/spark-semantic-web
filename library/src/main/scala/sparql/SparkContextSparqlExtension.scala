package sparkql

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SparkSession
import sparkql.FallbackHandler.fallback
import sparkql.QueryConverter.toSpark
import org.apache.spark.sql.Encoder

// Adds a `sparql` method to SparkSession
object SparkSessionSparqlExtension {

  implicit class SparqlEnhancedSession(spark: SparkSession) {

    def sparql(query: String, rdfFiles: Set[String]): DataFrame = {
      println(s"Running SPARQL query: $query")
      println(s"Files in play: $rdfFiles")

      if (supportsQuery(query)) {
        toSpark(query, rdfFiles)(spark)
      } else {
        println("Query not supported by Spark")
        fallback(query, rdfFiles)(spark)
      }
    }
  }

  def supportsQuery(query: String): Boolean = {
    // Stub for now: eventually inspect the query structure
    false
  }
}
