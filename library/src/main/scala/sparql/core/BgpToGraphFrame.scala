package sparql.core

import org.apache.jena.graph.{Node, Triple}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object BgpToGraphFrame {
  private sealed trait AliasInfo {
    def alias: String
  }
  private case class VertexAlias(alias: String) extends AliasInfo
  private case class LiteralAlias(alias: String, value: String)
      extends AliasInfo

  case class MotifFilter(motif: String, filter: String)

  def buildMotifAndFilter(triples: Seq[Triple]): MotifFilter = {
    val aliasMap = mutable.Map[String, AliasInfo]()
    var vertexCounter = 0

    def getKey(node: Node): String = {
      if (node.isVariable) s"?${node.getName}"
      else if (node.isURI) node.getURI
      else if (node.isLiteral) "\"" + node.getLiteralLexicalForm + "\""
      else node.toString
    }

    def getAlias(node: Node): String = {
      val key = getKey(node)

      aliasMap.get(key) match {
        case Some(info) => info.alias
        case None =>
          vertexCounter += 1
          val newAlias =
            if (node.isLiteral) s"lit$vertexCounter" else s"v$vertexCounter"
          val aliasInfo =
            if (node.isLiteral)
              LiteralAlias(newAlias, node.getLiteralLexicalForm)
            else
              VertexAlias(newAlias)
          aliasMap.put(key, aliasInfo)
          newAlias
      }
    }

    def extractUsedAliases(motifs: Seq[String]): Set[String] = {
      // Regexp to find aliases within parentheses
      val aliasPattern = "\\(([^)]+)\\)".r
      motifs.flatMap(m => aliasPattern.findAllMatchIn(m).map(_.group(1))).toSet
    }

    val motifs = ListBuffer[String]()
    val filters = ListBuffer[String]()

    triples.zipWithIndex.foreach { case (triple, edgeCounter) =>
      val sAlias = getAlias(triple.getSubject)
      val eAlias = s"e$edgeCounter"
      val oAlias = getAlias(triple.getObject)
      val pURI = triple.getPredicate.getURI

      motifs += s"($sAlias)-[$eAlias]->($oAlias)"
      filters += s"$eAlias.relationship = '$pURI'"
    }

    val usedAliases: Set[String] = extractUsedAliases(motifs)
    // Add literal filters for literal aliases used in the motif
    filters ++= aliasMap.values
      .collect {
        case LiteralAlias(alias, value) if usedAliases.contains(alias) =>
          val escapedValue = value.replace("'", "\\'")
          s"$alias.value = '$escapedValue'"
      }

    MotifFilter(
      motif = motifs.mkString(" ; "),
      filter = filters.mkString(" AND ")
    )
  }
}
