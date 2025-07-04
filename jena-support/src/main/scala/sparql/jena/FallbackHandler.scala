package sparql.jena

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Row
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.types.StructType
import org.graphframes.GraphFrame
import sparql.core.NoSuchModel

import scala.collection.JavaConverters._

object FallbackHandler {

  def fallback(query: String, graph: GraphFrame)(implicit
      spark: SparkSession
  ): DataFrame = {
    val model = JenaFrame.toJenaModel(graph)
    model match {
      case Some(model: Model) =>
        fallback(query, model)
      case None =>
        throw NoSuchModel()
    }
  }

  def fallback(query: String, rdfFiles: Set[String])(implicit
      spark: SparkSession
  ): DataFrame = {
    val model = ModelFactory.createDefaultModel()

    rdfFiles.foreach { filePath =>
      println(s"Loading file: $filePath")
      RDFDataMgr.read(model, filePath)
    }

    fallback(query, model)
  }

  def fallback(query: String, model: Model)(implicit
      spark: SparkSession
  ): DataFrame = {
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
