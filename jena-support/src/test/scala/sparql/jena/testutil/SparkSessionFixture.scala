package testutil

import munit.Fixture
import org.apache.spark.sql.SparkSession

object SparkSessionFixture extends Fixture[SparkSession]("spark") {
  private lazy val _spark: SparkSession = {
    val s = SparkSession.builder
      .appName("UnitTests")
      .master("local[1]")
      .config("spark.ui.enabled", "false")
      .config("spark.sql.shuffle.partitions", "1")
      .config("spark.sql.adaptive.enabled", "false")
      .config("spark.sql.codegen.wholeStage", "false")
      .getOrCreate()

    // warm-up
    import s.implicits._
    s.range(1).count()
    println("[spark warmup] done")
    s
  }

  def apply(): SparkSession = _spark
  override def beforeAll(): Unit = { val _ = _spark } // Force init
  override def afterAll(): Unit = _spark.stop()
}
