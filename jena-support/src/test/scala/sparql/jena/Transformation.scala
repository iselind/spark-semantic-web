package sparql.jena

import sparql.core.BgpToGraphFrame.buildMotifAndFilter
import sparql.core.ext
import sparql.core.ext.SparqlParser

/** Tests the conversion from sparql to QueryNode
  */
class Transformation extends munit.FunSuite {
  test("Single BGP test") {
    // This query means:
    // Query only matches the foaf:name triple and returns its components
    // ```
    // (?s, ?p, ?o)
    // ```
    // So for a subject like :Alice who has:
    // ```
    // :Alice foaf:name "Alice" .
    // :Alice rdf:type foaf:Person .
    // :Alice foaf:age 30 .
    // ```
    // we get the following response
    // ```
    // ?s      ?p         ?o
    // :Alice  foaf:name  "Alice"
    // ```
    val sparql =
      """
        |SELECT ?s ?p ?o WHERE {
        |  ?s <http://xmlns.com/foaf/0.1/name> ?name .
        |}
        |""".stripMargin

    val parser: SparqlParser = JenaSparqlParser
    val qn = parser.parse(sparql)
    val parsed = qn.where
    assert(parsed != null)

    // Make sure we only discovered one BGP
    assert(!parsed.aborted, parsed.abortReason)
    assert(!parsed.requiresFallback)
    assertListSize(parsed.filters, 0)
    assertListSize(parsed.unions, 0)
    assertListSize(parsed.optionals, 0)
    assertListSize(parsed.others, 0)
    assertListSize(parsed.bgp, 1)

    val myBgp = parsed.bgp.head
    val subject = myBgp.getMatchSubject
    val predicate = myBgp.getMatchPredicate
    val o = myBgp.getMatchObject

    println(myBgp)
    printNode(subject)
    printNode(predicate)
    printNode(o)

    assert(subject.isVariable)
    assertEquals(subject.getName, "s", subject)
    assert(predicate.isURI)
    assertEquals(predicate.getURI, "http://xmlns.com/foaf/0.1/name", subject)
    assert(o.isVariable)
    assertEquals(o.getName, "name", subject)
  }

  def printNode(node: ext.Node): Unit = {
    println(nodeToString(node))
  }

  def nodeToString(node: ext.Node): String = {
    /*
     * A Node has subtypes:
     * Node_Blank, Node_URI, Node_Literal, Node_Triple for RDF terms.
     * Node_Variable, Node_ANY, for variables and wildcard.
     * ARQs Var extends Node_Variable.
     * Node_Ext(extension), and Node_Graph outside RDF.
     */

    if (node == sparql.jena.Node.ANY) {
      "Any node (wildcard)"
    } else if (node.isVariable) {
      s"Variable node: ${node.getName}"
    } else if (node.isURI) {
      s"URI node: ${node.getURI}"
    } else if (node.isLiteral) {
      s"Literal node: ${node.getLiteralLexicalForm}"
    } else if (node.isBlank) {
      s"Blank node: ${node.getBlankNodeLabel}"
    } else {
      "Unknown node type"
    }
  }

  test("Double BGP test") {
    // This query means:
    // "Give me all triples about subjects that have a name."
    // So for a subject like :Alice who has:
    // ```
    // :Alice foaf:name "Alice" .
    // :Alice rdf:type foaf:Person .
    // :Alice foaf:age 30 .
    // ```
    // we get the following response
    // ```
    // ?s      ?p           ?o        ?name
    // :Alice  foaf:name    "Alice"   "Alice"
    // :Alice  rdf:type     foaf:Person "Alice"
    // :Alice  foaf:age     30         "Alice"
    // ```
    val sparql =
      """
        |SELECT ?s ?p ?o WHERE {
        |  ?s ?p ?o .
        |  ?s <http://xmlns.com/foaf/0.1/name> ?name .
        |}
        |""".stripMargin

    val parser: SparqlParser = JenaSparqlParser
    val qn = parser.parse(sparql)
    val parsed = qn.where
    assert(parsed != null)

    // Make sure we only discovered one BGP
    assert(!parsed.aborted, parsed.abortReason)
    assert(!parsed.requiresFallback)
    assertListSize(parsed.filters, 0)
    assertListSize(parsed.unions, 0)
    assertListSize(parsed.optionals, 0)
    assertListSize(parsed.others, 0)
    assertListSize(parsed.bgp, 2)
  }

  def assertListSize[T](lst: Seq[T], expectedLength: Int): Unit = {
    assertEquals(lst.size, expectedLength, lst)
  }

  test("less basic test") {
    val sparql =
      """
        |PREFIX : <http://example.org/>
        |SELECT ?s ?p ?o
        |WHERE {
        |  ?s ?p ?o .
        |  FILTER(?o > 5)
        |  OPTIONAL {
        |    ?o :email ?e .
        |    FILTER bound(?e)
        |  }
        |  {
        |    ?s a :Person
        |  } UNION {
        |    ?s :knows ?k .
        |    OPTIONAL { ?k :age ?a }
        |  }
        |}
        |""".stripMargin

    val parser: SparqlParser = JenaSparqlParser
    val parsed = parser.parse(sparql)
    assert(parsed != null)
    // printQueryNode(parsed)
  }

  test("BGP to motif and filter") {
    val triples = Seq(
      Triple.create(
        NodeFactory.createVariable("s"),
        NodeFactory.createURI("http://xmlns.com/foaf/0.1/name"),
        NodeFactory.createVariable("name")
      ),
      Triple.create(
        NodeFactory.createVariable("s"),
        NodeFactory.createURI("http://xmlns.com/foaf/0.1/age"),
        NodeFactory.createLiteral("25")
      )
    )

    val result = buildMotifAndFilter(triples)

    assertEquals(result.motif, "(v1)-[e0]->(v2) ; (v1)-[e1]->(lit3)")
    assertEquals(
      result.filter,
      "e0.relationship = 'http://xmlns.com/foaf/0.1/name' AND e1.relationship = 'http://xmlns.com/foaf/0.1/age' AND lit3.value = '25'"
    )
  }

  test("BGP from QueryNode to motif and filter") {
    val sparql =
      """
        |SELECT ?s ?name WHERE {
        |  ?s <http://xmlns.com/foaf/0.1/name> ?name .
        |}
        |""".stripMargin

    val parser: SparqlParser = JenaSparqlParser
    val qn = parser.parse(sparql)

    val result = buildMotifAndFilter(qn.where.bgp)

    assertEquals(result.motif, "(v1)-[e0]->(v2)")
    assertEquals(
      result.filter,
      "e0.relationship = 'http://xmlns.com/foaf/0.1/name'"
    )

    assertEquals(qn.select.vars, List("s", "name"))
    assertEquals(qn.select.uris, List())
  }
}
