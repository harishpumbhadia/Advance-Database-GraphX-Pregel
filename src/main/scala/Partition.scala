import org.apache.spark.graphx.{Graph, VertexId, Edge}
import org.apache.spark.graphx.util.GraphGenerators
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

object Partition {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Partition")
    val sc = new SparkContext(conf)

    val edge: RDD[Edge[Long]] = sc
      .textFile(args(0)) 
      .map(line => {
        val (node, adjacent) = line.split(",").splitAt(1)
        var vertexID = node(0).toLong
        (vertexID, adjacent.toList.map(_.toLong))
      })
      .flatMap(x => x._2.map(y => (x._1, y)))
      .map(n => {
        Edge(n._1, n._2, 0L)
      })

    var counter = 0
    val xx = sc
      .textFile(args(0))
      .map(line => {
        val (node, _) = line.split(",").splitAt(1)
        var ver = -1L
        if (counter < 5) {
          ver = node(0).toLong
          counter += 1
        }
        ver 
      })

    val xxfinal = xx.filter(_ != -1).collect().toList

   val graph: Graph[Long, Long] = Graph
      .fromEdges(edge, 0L)
      .mapVertices((id, _) => {
        var centroid = -1L
        if (xxfinal.contains(id)) {
          centroid = id
        }
        centroid
      })

    val i = graph.pregel(Long.MinValue, 6)(
      (vertexid, vertexData, candidateCluster) => {
        if (vertexData == -1) {
          math.max(vertexData, candidateCluster)
        } else {
          vertexData
        }
      },
      triplet => {
        Iterator((triplet.dstId, triplet.srcAttr))
      },
      (x, y) => math.max(x, y)
    )

    var partitionSize = i.vertices
      .map {
        case (id, centroid) =>
          (centroid, 1)
      }
      .reduceByKey(_ + _)

    partitionSize.collect.foreach(println)
  }
}