package sparql.core.query

import org.apache.spark.sql.Column
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col
import org.graphframes.GraphFrame
import sparql.core.GraphResolver
import sparql.core.ext.Node

case class SelectNode(vars: List[String], uris: List[Node], where: WhereNode) extends Executor[DataFrame] {
  override def run(aliasMap: Map[String, String], graphResolver: GraphResolver)(frame: Option[GraphFrame]): DataFrame = {
    val f = where.run(aliasMap = aliasMap, graphResolver = graphResolver)(frame)

    val realKeyMap: List[Column] = aliasMap.view
      .filterKeys(vars.toSet)
      .map { case (k, v) => col(v).alias(k) }
      .toList

    // .map { case (k, v) => col(v).alias(k) }

    f.vertices.select(realKeyMap: _*)
  }
}
