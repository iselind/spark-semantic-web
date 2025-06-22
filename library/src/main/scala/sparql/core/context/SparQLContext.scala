package sparql.core.context

import sparql.core.ext.SparqlParser
import sparql.core.{GraphResolver, GraphStore, SparqlExecutionStrategy}

case class SparQLContext(
    sparqlParser: SparqlParser,
    graphResolver: GraphResolver,
    graphStore: GraphStore,
    execStrategy: SparqlExecutionStrategy
)
