package sparql.core.exception

case class ParsingAborted(abortReason: String) extends Throwable {}
