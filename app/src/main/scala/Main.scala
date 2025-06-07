import org.apache.spark.{SparkConf, SparkContext}
import SparkContextSparqlExtension._ // Import the implicit class

object Main {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("SparqlExample").setMaster("local[*]")
    val sc = new SparkContext(conf)

    // Now you can use the new `sparql` method
    sc.sparql("""
      PREFIX foaf: <http://xmlns.com/foaf/0.1/>
      SELECT ?name WHERE { ?person foaf:name ?name }
    """)

    sc.stop()
  }
}

