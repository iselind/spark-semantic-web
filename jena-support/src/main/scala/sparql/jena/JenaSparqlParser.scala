package sparql.jena

import sparql.core.ext.ParsedQuery
import sparql.core.ext.SparqlParser
import sparql.core.query.Executor

object JenaSparqlParser extends SparqlParser {

  override def parse(query: String): ParsedQuery = ???


  /*
  def parse(query: String): QueryNode = {
    val q = QueryFactory.create(query)
    q.queryType() match {
      case QueryType.SELECT => parseSelect(q)
      case _ =>
        throw UnsupportedQuery("Only SELECT statements are supported for now")
    }
  }
  */
  def parse[T](query: String): Executor[T] ={
    /*
    val q = QueryFactory.create(query)
    val r: Executor[DataFrame] = parseSelect(q)

    r match {
      case e: Executor[T] => e
      case _ => throw new IllegalArgumentException()
    }
    */
    ???
  } 

  /*
  private def parseSelect(q: Query): SelectNode = {
    val queryPattern = q.getQueryPattern

    val vars = q.getResultVars.asScala.toList
    val uris = q.getResultURIs.asScala.toList.map(new Node(_))
    SelectNode(vars, uris, parseElement(queryPattern))
  }

  private def parseElement(element: Element): WhereNode = {
    var aborted = false
    var reason: Option[String] = None

    val bgp = scala.collection.mutable.ListBuffer[TriplePath]()
    val filters = scala.collection.mutable.ListBuffer[Expr]()
    val unions = scala.collection.mutable.ListBuffer[List[WhereNode]]()
    val optionals = scala.collection.mutable.ListBuffer[WhereNode]()
    val others = scala.collection.mutable.ListBuffer[String]()

    val fallbackMarkers =
      Set("ElementService", "ElementBind", "ElementMinus", "ElementSubQuery")

    def walk(el: Element): Unit = {
      if (aborted) return

      el match {
        case g: ElementGroup =>
          g.getElements.asScala.foreach(walk)

        case tb: ElementTriplesBlock =>
          bgp ++= tb.getPattern.getList.asScala.map(_.asInstanceOf[TriplePath])

        case f: ElementFilter =>
          filters += f.getExpr

        case o: ElementOptional =>
          optionals += parseElement(o.getOptionalElement)

        case u: ElementUnion =>
          val parsedBranches = u.getElements.asScala.map(parseElement).toList
          unions += parsedBranches

        case pathBlock: ElementPathBlock =>
          bgp ++= pathBlock.getPattern.asScala

        case other =>
          val className = other.getClass.getSimpleName
          others += className
          if (fallbackMarkers.contains(className)) {
            aborted = true
            reason = Some(s"Unsupported element encountered: $className")
          }
      }
    }

    walk(element)
    WhereNode(
      bgp = bgp.toList.map(sparql.jena.Triple.from),
      filters = filters.toList.map(sparql.jena.FilterExpression.from),
      unions = unions.toList,
      optionals = optionals.toList,
      others = others.toList,
    )
  }
  */
}
