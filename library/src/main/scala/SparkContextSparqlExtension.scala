import org.apache.spark.sql.SparkSession
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.spark.sql.Dataset

// Adds a `sparql` method to SparkSession
object SparkSessionSparqlExtension {

  implicit class SparqlEnhancedSession(spark: SparkSession) {

    def sparql(query: String, rdfFiles: Set[String]): Dataset[String] = {
      println(s"Running SPARQL query: $query")
      println(s"Files in play: $rdfFiles")

      if (supportsQuery(query)) {
        toSpark(query, rdfFiles)
      } else {
        fallback(query, rdfFiles)
      }
    }

    def toSpark(query: String, rdfFiles: Set[String]): Dataset[String] = {
      import spark.implicits._

      // TODO: Translate query to Spark logic

      Seq.empty[String].toDS()
    }

    def fallback(query: String, rdfFiles: Set[String]): Dataset[String] = {
      println("Using fallback")

      val model = ModelFactory.createDefaultModel()
      // load your RDF model here (from file, DB, etc.)

      // Load each RDF file into the model
      rdfFiles.foreach { filePath =>
        println(s"Loading file: $filePath")
        RDFDataMgr.read(model, filePath)
      }

      val queryExec = QueryExecutionFactory.create(query, model)
      val results = queryExec.execSelect()
      import spark.implicits._
      Iterator
        .continually(if (results.hasNext) Some(results.next()) else None)
        .takeWhile(_.isDefined)
        .flatten
        .map(_.getLiteral("name").getString)
        .toSeq
        .toDS()
    }
  }

  def supportsQuery(query: String): Boolean = {
    println("Query not supported by Spark")

    // Stub for now: eventually inspect the query structure
    false
  }
}
