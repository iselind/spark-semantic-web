package sparkql

import org.apache.spark.sql.{DataFrame, SparkSession}
import sparkql.FallbackHandler.fallback
import sparkql.RecursiveSparqlParser.parseElement
import org.apache.jena.query.QueryFactory
import org.apache.jena.sparql.expr.Expr
import org.apache.jena.sparql.core.TriplePath

object QueryConverter {

  def toSpark(query: String, rdfFiles: Set[String])(implicit
      spark: SparkSession
  ): DataFrame = {
    import spark.implicits._

    // TODO: Translate query to Spark logic
    // Convert the SparQL query into a decomposed AST-like structure.
    // Feed that to the overloaded `toSpark()`
    val q = QueryFactory.create(query)
    val queryPattern = q.getQueryPattern

    val parsed = parseElement(queryPattern)
    toSpark(parsed, rdfFiles)(spark)

    // A very temporary solution
    fallback(query, rdfFiles)
  }

  // Converts a SparQL query into a decomposed AST-like structure
  def toSpark(query: QueryNode, rdfFiles: Set[String])(implicit
      spark: SparkSession
  ): DataFrame = {
    throw new NotImplementedError()
  }
}
