import sparkql.SparkSessionSparqlExtension.supportsQuery
// For more information on writing tests, see
// https://scalameta.org/munit/docs/getting-started.html
class QuerySupport extends munit.FunSuite {
  test("supports query always return false") {
    assertEquals(false, supportsQuery(""))
  }
}
