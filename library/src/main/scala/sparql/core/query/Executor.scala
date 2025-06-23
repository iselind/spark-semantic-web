package sparql.core.query

import org.graphframes.GraphFrame
import sparql.core.GraphResolver

trait Executor[T] {
  def run(aliasMap: Map[String, String], graphResolver: GraphResolver)(
      frame: Option[GraphFrame]
  ): T
}
