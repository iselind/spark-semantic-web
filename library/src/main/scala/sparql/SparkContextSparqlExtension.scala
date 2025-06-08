package sparkql

import org.apache.spark.sql.SparkSession
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.spark.sql.Dataset
import sparkql.QueryConverter.toSpark
import sparkql.FallbackHandler.fallback

// Adds a `sparql` method to SparkSession
object SparkSessionSparqlExtension {

  implicit class SparqlEnhancedSession(spark: SparkSession) {

    def sparql(query: String, rdfFiles: Set[String]): Dataset[String] = {
      println(s"Running SPARQL query: $query")
      println(s"Files in play: $rdfFiles")

      if (supportsQuery(query)) {
        toSpark(query, rdfFiles)(spark)
      } else {
        fallback(query, rdfFiles)(spark)
      }
    }

    def supportsQuery(query: String): Boolean = {
      println("Query not supported by Spark")

      // Stub for now: eventually inspect the query structure
      false
    }
  }
}
