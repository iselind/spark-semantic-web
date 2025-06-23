import munit.FunSuite
import org.apache.spark.sql.SparkSession
import sparql.core.BasicGraphResolver
import sparql.core.SparkSessionSparqlExtension._
import sparql.core.context.SparQLContext
import sparql.core.graphstore.MapGraphStore
import sparql.jena.executionstrategy.JenaOnlyStrategy
import sparql.jena.JenaFrame
import sparql.jena.JenaSparqlParser

class SparqlQuerySuite extends FunSuite {

  private val sparkSession: SparkSession = SparkSession
    .builder()
    .appName("SparqlTestSuite")
    .master("local[*]")
    .getOrCreate()

  import sparkSession.implicits._

  val graphStore = new MapGraphStore
  private implicit val sparqlContext: SparQLContext = SparQLContext(
    JenaSparqlParser,
    new BasicGraphResolver(graphStore),
    graphStore,
    JenaOnlyStrategy
  )

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

    val actualNames: List[String] = sparkSession
      .sparql(query)
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
