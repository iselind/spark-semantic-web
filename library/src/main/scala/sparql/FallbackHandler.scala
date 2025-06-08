package sparkql

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.spark.sql.{Dataset, DataFrame, SparkSession}
import org.apache.jena.query.QueryFactory
import scala.collection.JavaConverters._
import org.apache.spark.sql.Encoder
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.types.StringType

object FallbackHandler {
  def fallback(query: String, rdfFiles: Set[String])(implicit
      spark: SparkSession
  ): DataFrame = {
    println("Using fallback")

    val model = ModelFactory.createDefaultModel()

    rdfFiles.foreach { filePath =>
      println(s"Loading file: $filePath")
      RDFDataMgr.read(model, filePath)
    }

    val parsedQuery = QueryFactory.create(query)
    val resultVars = parsedQuery.getResultVars.asScala.toSeq

    val queryExec = QueryExecutionFactory.create(query, model)
    val results = queryExec.execSelect()

    val rows = Iterator
      .continually(if (results.hasNext) Some(results.next()) else None)
      .takeWhile(_.isDefined)
      .flatten
      .map { qs =>
        Row.fromSeq(
          resultVars.map(varName =>
            Option(qs.get(varName)).map(_.toString).orNull
          )
        )
      }
      .toSeq

    val schema = StructType(
      resultVars.map(name => StructField(name, StringType, nullable = true))
    )
    spark.createDataFrame(spark.sparkContext.parallelize(rows), schema)
  }
}
