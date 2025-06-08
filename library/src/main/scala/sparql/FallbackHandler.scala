package sparkql

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.spark.sql.{Dataset, SparkSession}

object FallbackHandler {
  def fallback(query: String, rdfFiles: Set[String])(implicit
      spark: SparkSession
  ): Dataset[String] = {
    println("Using fallback")

    val model = ModelFactory.createDefaultModel()

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
