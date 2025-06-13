package sparql

import org.apache.spark.sql.{DataFrame, SparkSession}

trait SparqlExecutionStrategy {
  def execute(query: String)(implicit spark: SparkSession): DataFrame
}
