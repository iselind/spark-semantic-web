package sparql.jena

import munit.FunSuite
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Row
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.CatalystTypeConverters
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import sparql.core.SCompiler
import sparql.jena.JenaFrontEnd

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.LongAdder

class SparqlSparkSuite extends FunSuite {
  override def munitFixtures = List(testutil.SparkSessionFixture)
  private def spark: SparkSession = testutil.SparkSessionFixture()

  // Setup a sample "quads" DataFrame, mimicking your RDF store schema
  def setupQuads(spark: SparkSession): Unit = {
    import spark.implicits._

    val df = Seq(
      ("subject1", "predicate1", "object1", "graph1"),
      ("subject2", "predicate2", "object2", "graph1"),
      ("subject3", "predicate3", "object3", "graph2")
    ).toDF("s", "p", "o", "g")

    // Register as a temp view for your compiler to consume
    time("register view", df.createOrReplaceTempView("quads"))
  }

  test("SPARQL SELECT query compiles and runs") {
    time("setup quads", setupQuads(spark))

    val sparqlQuery =
      """
        |SELECT ?s ?p WHERE {
        |  ?s ?p ?o .
        |  FILTER (?o != "object2")
        |}
      """.stripMargin

    val sb = time("compile", JenaFrontEnd.compile(sparqlQuery))
    val rawPlan = SCompiler.compile(sb.result())

    val logicalPlan = rawPlan transformUp {
      case _: org.apache.spark.sql.catalyst.plans.logical.LocalRelation =>
        val startDF = spark.table("quads")
        startDF.queryExecution.logical
    }

    assert(logicalPlan != null)
    assert(
      logicalPlan
        .isInstanceOf[
          org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
        ]
    )

    val df = time("to DF", logicalPlanToDataFrame(logicalPlan))
    val count = time("df collection", df.count())

    assertEquals(count, 2L)
    println("Total Elapsed time: " + totalTime.sum() / 1000000f / 1000f + "s")
  }

  val totalTime = new LongAdder()
  val idStack = new ConcurrentLinkedDeque[String]()

  def time[T](id: String, block: => T): T = {
    idStack.push(id)
    val before = System.nanoTime
    val result = block
    val after = System.nanoTime
    idStack.pop()

    val diff = after - before
    if (idStack.isEmpty())
      totalTime.add(diff)
    println(
      " ".repeat(idStack.size()) +
        "[" + id + "] Elapsed time: " + diff / 1000000f / 1000f + "s"
    )
    result
  }

  def logicalPlanToDataFrame(
      plan: LogicalPlan
  ): DataFrame = {
    // 1. Let Spark turn the logical plan into a physical RDD of InternalRow
    val qe = spark.sessionState.executePlan(plan) // QueryExecution
    val rdd = qe.toRdd // RDD[InternalRow]

    // 2. Convert InternalRow → Row and build a DataFrame with the same schema
    val schema = qe.analyzed.schema
    val toScala = CatalystTypeConverters
      .createToScalaConverter(schema)
      .asInstanceOf[InternalRow => Row]
    val rowRDD = rdd.mapPartitions(_.map(toScala))
    // Create explicit encoder to avoid having Spark do reflection and schema analysis
    val encoder = Encoders.row(schema)
    spark.createDataset(rowRDD)(encoder)
  }
}
