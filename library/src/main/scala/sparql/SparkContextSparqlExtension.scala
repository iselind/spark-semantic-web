package sparkql

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.sparql.syntax._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.Encoder
import org.apache.spark.sql.SparkSession
import sparkql.FallbackHandler.fallback
import sparkql.QueryConverter.toSpark

import scala.collection.JavaConverters._
import org.apache.jena.query.QueryParseException

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
    isPureBGP(query)
  }

  /** A Basic Graph Pattern is essentially a conjunction of triple patterns. In
    * SPARQL terms <pre> SELECT ?s ?p ?o WHERE { ?s ?p ?o . ?s
    * <http://xmlns.com/foaf/0.1/name> ?name . } </pre> This is a pure BGP: just
    * a list of triple patterns.
    */
  def isPureBGP(query: String): Boolean = {
    try {
      val parsedQuery = QueryFactory.create(query)
      val queryPattern = parsedQuery.getQueryPattern

      isPureBGP(queryPattern)
    } catch {
      case e: QueryParseException => false
    }
  }

  def isPureBGP(element: Element): Boolean = element match {
    case _: ElementTriplesBlock =>
      true
    case _: ElementPathBlock =>
      true
    case group: ElementGroup =>
      group.getElements.asScala.forall {
        case _: ElementTriplesBlock => true
        case _: ElementPathBlock    => true
        case _ => {
          false
        }
      }
    case _ =>
      false
  }

}
