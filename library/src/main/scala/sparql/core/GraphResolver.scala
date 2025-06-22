package sparql.core

import org.graphframes.GraphFrame

trait GraphResolver {

  /** Resolve URIs for named graphs.
    * @param uri
    *   Graph designation.
    *
    * Use the format <pre>{schema}://{graph-name}</pre> where schema "spark", or
    * possibly "local" (I've not decided yet**), means to look in the local
    * graph store.
    *
    * If null and the graph store contains exactly one named graph, return that
    * graph regardless of name.
    * @return
    *   Some(GraphFrame) if a graph with the provided uri was found, and it was
    *   possible to convert it to a GraphFrame.
    */
  def getGraph(uri: String): Option[GraphFrame]
}

class BasicGraphResolver extends GraphResolver {

  /** Resolve URIs for named graphs.
    *
    * @param uri
    *   Graph designation.
    *
    * Use the format <pre>{schema}://{graph-name}</pre> where schema "spark", or
    * possibly "local" (I've not decided yet**), means to look in the local
    * graph store.
    *
    * If null and the graph store contains exactly one named graph, return that
    * graph regardless of name.
    * @return
    *   Some(GraphFrame) if a graph with the provided uri was found, and it was
    *   possible to convert it to a GraphFrame.
    */
  override def getGraph(uri: String): Option[GraphFrame] = ???
}
