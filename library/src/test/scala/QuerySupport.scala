import sparkql.SparkSessionSparqlExtension.supportsQuery
import sparkql.SparkSessionSparqlExtension.isPureBGP
import sparkql.SparkSessionSparqlExtension
// For more information on writing tests, see
// https://scalameta.org/munit/docs/getting-started.html
class QuerySupport extends munit.FunSuite {
  test("supports query always return false") {
    assertEquals(false, supportsQuery(""))
  }

  test("is a BGP") {
    val queries = Seq(
      """
      SELECT ?s ?p ?o WHERE {
        ?s ?p ?o .
        ?s <http://xmlns.com/foaf/0.1/name> ?name .
      }
    """,
      """
      PREFIX foaf: <http://xmlns.com/foaf/0.1/>
      SELECT ?person ?name WHERE {
        ?person a foaf:Person .
        ?person foaf:name ?name .
      }
    """
    )

    queries.zipWithIndex.foreach { case (query: String, index: Int) =>
      assert(
        isPureBGP(query),
        s"Index $index was incorrectly identified as NOT a pure BGP"
      )
    }
  }

  test("is NOT a BGP") {
    // Why these are NOT BGPs:
    // The first query has a filter
    // The second query uses union
    val queries = Seq(
      """
      SELECT ?s ?p ?o WHERE {
        ?s ?p ?o .
        FILTER(?p = rdf:type)
      }
    """,
      """
      PREFIX foaf: <http://xmlns.com/foaf/0.1/>
      SELECT ?person ?name WHERE {
        {
          ?person a foaf:Person .
        } UNION {
          ?person foaf:name ?name .
        }
      }
    """
    )

    queries.zipWithIndex.foreach { case (query, index) =>
      assert(
        !isPureBGP(query),
        s"Index $index was incorrectly identified as a pure BGP"
      )
    }
  }
}
