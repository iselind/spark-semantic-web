package sparkql

import org.apache.spark.sql.{Dataset, SparkSession}

object QueryConverter {
  def toSpark(query: String, rdfFiles: Set[String])(implicit
      spark: SparkSession
  ): Dataset[String] = {
    import spark.implicits._

    // TODO: Translate query to Spark logic
    println("Query not supported by Spark")

    Seq.empty[String].toDS()
  }
}
