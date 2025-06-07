import org.apache.spark.sql.SparkSession
import SparkSessionSparqlExtension._

object Main {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("SparqlExample")
      .master("local[*]")
      .getOrCreate()

    spark.sparql("""
      PREFIX foaf: <http://xmlns.com/foaf/0.1/>
      SELECT ?name WHERE { ?person foaf:name ?name }
    """)
  }
}

