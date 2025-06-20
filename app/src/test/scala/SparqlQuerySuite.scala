import munit.FunSuite
import org.apache.spark.sql.SparkSession
import sparql.core.SparkSessionSparqlExtension._
import sparql.core.SparqlExecutionStrategy
import sparql.jena.JenaFrame
import sparql.jena.executionstrategy.JenaOnlyStrategy

class SparqlQuerySuite extends FunSuite {

  private lazy val sparkSession: SparkSession = SparkSession
    .builder()
    .appName("SparqlTestSuite")
    .master("local[*]")
    .getOrCreate()

  import sparkSession.implicits._

  override def afterAll(): Unit = {
    sparkSession.stop()
  }

  test("SPARQL query executed by Jena returns expected names") {
    val rdfFiles = Set("data/example1.ttl", "data/example2.rdf")
    val frame = JenaFrame.toGraphFrame(rdfFiles)(sparkSession)
    sparkSession.registerGraph(frame, "Jena")

    val query =
      """
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        SELECT ?name WHERE { ?person foaf:name ?name }
      """

    val strategy: SparqlExecutionStrategy = JenaOnlyStrategy
    val actualNames: List[String] = sparkSession
      .sparql(query)(strategy)
      .select("name")
      .as[String]
      .collect()
      .toList
      .sorted

    val expectedNames: List[String] =
      List("Alice", "Bob", "Carol", "Dave").sorted

    assertEquals(actualNames, expectedNames)
  }
}
