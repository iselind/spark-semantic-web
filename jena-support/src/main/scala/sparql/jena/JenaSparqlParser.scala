package sparql.jena

import org.apache.jena.query.QueryFactory
import org.apache.jena.sparql.core.TriplePath
import org.apache.jena.sparql.expr.Expr
import org.apache.jena.sparql.syntax._
import sparql.core.ext.SparqlParser
import sparql.core.query.{QueryNode, SelectNode, WhereNode}

import scala.collection.JavaConverters._

object JenaSparqlParser extends SparqlParser {

  def parse(query: String): QueryNode = {
    val q = QueryFactory.create(query)
    val queryPattern = q.getQueryPattern

    val vars = q.getResultVars.asScala.toList
    val uris = q.getResultURIs.asScala.toList.map(new Node(_))
    QueryNode(SelectNode(vars, uris), parseElement(queryPattern))
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
      aborted = aborted,
      abortReason = reason,
      requiresFallback = aborted || others.nonEmpty
    )
  }

}
