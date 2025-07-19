package sparql.jena

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Row
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.CatalystTypeConverters
import org.apache.spark.sql.catalyst.InternalRow
import sparql.core.QueryPlan
import sparql.core.SCompiler

case class SparqlQuery(private val qp: QueryPlan) extends (DataFrame => DataFrame) {

  private val rawPlan = SCompiler.compile(qp)

  def apply(df: DataFrame): DataFrame = {
    val spark = df.sparkSession
    val plan = rawPlan transformUp {
      case _: org.apache.spark.sql.catalyst.plans.logical.LocalRelation =>
        df.queryExecution.logical
    }

    // 1. Let Spark turn the logical plan into a physical RDD of InternalRow
    val qe = spark.sessionState.executePlan(plan) // QueryExecution
    val rdd = qe.toRdd // RDD[InternalRow]

    // 2. Convert InternalRow → Row and build a DataFrame with the same schema
    val schema = qe.analyzed.schema
    val toScalaa = CatalystTypeConverters
      .createToScalaConverter(schema)
      .asInstanceOf[InternalRow => Row]
    val rowRDD = rdd.mapPartitions(_.map(toScalaa))

    val encoder = Encoders.row(schema)
    // Create explicit encoder to avoid having Spark do reflection and schema analysis
    spark.createDataset(rowRDD)(encoder)
  }
}