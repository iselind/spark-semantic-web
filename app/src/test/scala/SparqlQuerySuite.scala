import munit.FunSuite
import org.apache.spark.sql.SparkSession
import sparkql.SparkSessionSparqlExtension._

class SparqlQuerySuite extends FunSuite {

  private lazy val sparkSession: SparkSession = SparkSession
    .builder()
    .appName("SparqlTestSuite")
    .master("local[*]")
    .getOrCreate()

  override def afterAll(): Unit = {
    sparkSession.stop()
  }

  test("SPARQL query returns expected names") {
    val rdfFiles = Set("data/example1.ttl", "data/example2.rdf")
    val query =
      """
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        SELECT ?name WHERE { ?person foaf:name ?name }
      """

    import sparkSession.implicits._

    val actualNames: List[String] = sparkSession
      .sparql(query, rdfFiles)
      .select(
        "value" // XXX: The column will have the name "value", and not "name" which should be expected based on the query
      )
      .as[String]
      .collect()
      .toList
      .sorted

    val expectedNames: List[String] =
      List("Alice", "Bob", "Carol", "Dave").sorted

    assertEquals(actualNames, expectedNames)
  }
}
