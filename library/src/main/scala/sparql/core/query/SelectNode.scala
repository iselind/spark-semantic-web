package sparql.core.query

import org.apache.spark.sql.DataFrame
import org.graphframes.GraphFrame
import sparql.core.GraphResolver
import sparql.core.ext.Node

case class SelectNode(vars: List[String], uris: List[Node], where: WhereNode) extends Executor[DataFrame] {
  override def run(aliasMap: Map[String, String], graphResolver: GraphResolver)(frame: Option[GraphFrame]): DataFrame = {
    /*
    val f = where.run(aliasMap = aliasMap, graphResolver = graphResolver)(frame)

    val varsSet = vars.toSet
    val realKeyMap: List[Column] = aliasMap.view
      .filter { case (k, _) => varsSet.contains(k) }
      .map { case (k, v) => col(v).alias(k) }
      .toList

    f.vertices.select(realKeyMap: _*)

    // I have no clue what to do with the parameter "uris"
    */
    ???
  }
}
