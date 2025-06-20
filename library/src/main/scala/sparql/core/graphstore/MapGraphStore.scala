package sparql.core.graphstore

import org.graphframes.GraphFrame
import sparql.core.GraphStore

class MapGraphStore extends GraphStore {
  private val graphs = scala.collection.mutable.Map.empty[String, GraphFrame]

  override def getGraph(name: String): Option[GraphFrame] = graphs.get(name)

  override def putGraph(name: String, graph: GraphFrame): Unit =
    graphs.update(name, graph)

  override def removeGraph(name: String): Unit = graphs.remove(name)

  override def listGraphs(): Seq[String] = graphs.keys.toSeq
}
