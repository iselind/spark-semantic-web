package sparql.core

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SparkSession

trait SparqlExecutionStrategy {
  def execute(query: String)(implicit spark: SparkSession): DataFrame
}
