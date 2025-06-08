import org.apache.spark.sql.SparkSession
import sparkql.SparkSessionSparqlExtension._

object Main {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("SparqlExample")
      .master("local[*]")
      .getOrCreate()

    val rdfFiles = Set("data/example1.ttl", "data/example2.rdf")
    val query = """
      PREFIX foaf: <http://xmlns.com/foaf/0.1/>
      SELECT ?name WHERE { ?person foaf:name ?name }
    """
    spark
      .sparql(query, rdfFiles)
      .show()
  }
}
