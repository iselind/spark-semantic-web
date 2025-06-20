package sparql.core

import org.apache.jena.query.QueryFactory
import org.apache.jena.sparql.core.TriplePath
import org.apache.jena.sparql.expr.Expr
import org.apache.jena.sparql.syntax._

import scala.collection.JavaConverters._

// Representation of recursive query structure with fallback tracking
case class QueryNode(
    bgp: List[TriplePath] = List(),
    filters: List[Expr] = List(),
    unions: List[List[QueryNode]] = List(),
    optionals: List[QueryNode] = List(),
    others: List[String] = List(),
    aborted: Boolean = false,
    abortReason: Option[String] = None,
    requiresFallback: Boolean = false
)

object SparqlParser {

  def parse(query: String): QueryNode = {
    val q = QueryFactory.create(query)
    val queryPattern = q.getQueryPattern

    parseElement(queryPattern)
  }

  def parseElement(element: Element): QueryNode = {
    var aborted = false
    var reason: Option[String] = None

    val bgp = scala.collection.mutable.ListBuffer[TriplePath]()
    val filters = scala.collection.mutable.ListBuffer[Expr]()
    val unions = scala.collection.mutable.ListBuffer[List[QueryNode]]()
    val optionals = scala.collection.mutable.ListBuffer[QueryNode]()
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
    QueryNode(
      bgp = bgp.toList,
      filters = filters.toList,
      unions = unions.toList,
      optionals = optionals.toList,
      others = others.toList,
      aborted = aborted,
      abortReason = reason,
      requiresFallback = aborted || others.nonEmpty
    )
  }

  def printQueryNode(node: QueryNode, indent: String = ""): Unit = {
    if (node.aborted) {
      println(
        s"${indent}⛔ Parsing Aborted: ${node.abortReason.getOrElse("Unknown reason")}"
      )
      return
    }
    if (node.requiresFallback) {
      println(s"${indent}⚠️ Query requires fallback to Jena execution")
    }
    if (node.bgp.nonEmpty) {
      println(s"${indent}BGP:")
      node.bgp.foreach(t => println(s"${indent}  $t"))
    }
    if (node.filters.nonEmpty) {
      println(s"${indent}FILTERs:")
      node.filters.foreach(f => println(s"${indent}  $f"))
    }
    if (node.optionals.nonEmpty) {
      println(s"${indent}OPTIONALs:")
      node.optionals.foreach(o => printQueryNode(o, indent + "  "))
    }
    if (node.unions.nonEmpty) {
      println(s"${indent}UNIONs:")
      node.unions.foreach { branchList =>
        println(s"${indent}  UNION Branches:")
        branchList.foreach(branch => printQueryNode(branch, indent + "    "))
      }
    }
    if (node.others.nonEmpty) {
      println(s"${indent}Other elements:")
      node.others.foreach(o => println(s"${indent}  $o"))
    }
  }
}
