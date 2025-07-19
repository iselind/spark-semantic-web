package sparql.jena

import munit.FunSuite
import org.apache.spark.sql.SparkSession

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


    val q = JenaFrontEnd.compile(sparqlQuery)
    val df = q(spark.table("quads"))

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

}
