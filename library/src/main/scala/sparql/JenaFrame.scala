package sparql

import org.apache.jena.rdf.model.{Model, ModelFactory, Statement}
import org.apache.jena.riot.RDFDataMgr
import org.apache.spark.sql.SparkSession
import org.graphframes.GraphFrame

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ArrayBuffer

object JenaFrame {
  private val resolver: TrieMap[GraphFrame, Model] = TrieMap.empty

  def toJenaModel(graph: GraphFrame): Option[Model] = {
    resolver.get(graph)
  }

  def toGraphFrame(
      rdfFiles: Set[String]
  )(implicit spark: SparkSession): GraphFrame = {
    val model = ModelFactory.createDefaultModel()

    rdfFiles.foreach { filePath =>
      println(s"Loading file: $filePath")
      RDFDataMgr.read(model, filePath)
    }

    toGraphFrame(model)
  }

  private def toGraphFrame(
      model: Model
  )(implicit spark: SparkSession): GraphFrame = {
    import spark.implicits._

    // Extract triples from Jena Model
    val statements = model.listStatements()

    val triples: ArrayBuffer[(String, String, String)] =
      ArrayBuffer.empty[(String, String, String)]

    while (statements.hasNext) {
      val stmt: Statement = statements.nextStatement()
      val subj = stmt.getSubject.toString
      val predicate = stmt.getPredicate.toString
      val obj =
        if (stmt.getObject.isResource)
          stmt.getObject.asResource().toString
        else
          stmt.getObject.toString // For literals

      triples += ((subj, predicate, obj))
    }

    // Convert to Spark DataFrame
    val triplesDF = triples.toDF("subject", "predicate", "object")

    // Create vertices: distinct subject/object URIs
    val vertices = triplesDF
      .select($"subject".as("id"))
      .union(triplesDF.select($"object".as("id")))
      .distinct()

    // Create edges
    val edges = triplesDF
      .select(
        $"subject".as("src"),
        $"object".as("dst"),
        $"predicate".as("relationship")
      )

    // Create GraphFrame
    val g = GraphFrame(vertices, edges)
    resolver += (g -> model)
    g
  }

}
