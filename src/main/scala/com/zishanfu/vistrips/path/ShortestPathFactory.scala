package com.zishanfu.vistrips.path

import org.apache.spark.graphx.Graph
import org.apache.spark.graphx.lib.ShortestPaths
import org.apache.spark.sql.Row

import com.vividsolutions.jts.geom.Point
import com.zishanfu.vistrips.network.Link
import org.apache.spark.graphx.Pregel
import org.apache.spark.graphx.EdgeDirection
import org.apache.spark.graphx.EdgeTriplet
import org.apache.spark.sql.SparkSession
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.Coordinate
import org.apache.spark.sql.Encoders
import org.apache.spark.graphx.Edge

object ShortestPathFactory {
//Node: (id, lat, lon)
//A (4347874712, 33.4074225, -111.9389839)
//B (6058475294,  33.4074298, -111.9385580)
//C (41883108, 33.4074352, -111.9383137)
//
//D (5662664854, 33.4071662, -111.9383377)
//E (5662664855,  33.4069101, -111.9383463)
//
//F(5662664858, 33.4068765, -111.9384113)
//G(5662664859, 33.4064041, -111.9384314)
//
//H(5662664860, 33.4063666, -111.9393425)
//I(5662664861, 33.4063538, -111.9398013)
//
//J(3983483491, 33.4067486, -111.9398081)
  val gf = new GeometryFactory()
  
  def createPoint(lon : Double, lat: Double, id: Long) : Point = {
    val p = gf.createPoint(new Coordinate(lon, lat))
    p.setUserData(id)
    p
  }
  
  def runRandomGraph(sparkSession : SparkSession) : Unit = {
    
    val sc = sparkSession.sparkContext
    val map = Map("A" -> createPoint(-111.9389839, 33.4074225, 4347874712L),
                  "B" -> createPoint(-111.9385580, 33.4074298, 6058475294L),
                  "C" -> createPoint(-111.9383137, 33.4074352, 41883108L),
                  "D" -> createPoint(-111.9383377, 33.4071662, 5662664854L),
                  "E" -> createPoint(-111.9383463, 33.4069101, 5662664855L),
                  "F" -> createPoint(-111.9384113, 33.4068765, 5662664858L),
                  "G" -> createPoint(-111.9384314, 33.4064041, 5662664859L),
                  "H" -> createPoint(-111.9393425, 33.4063666, 5662664860L),
                  "I" -> createPoint(-111.9398013, 33.4063538, 5662664861L),
                  "J" -> createPoint(-111.9398081, 33.4067486, 3983483491L))
    val nodeRDD = sc.parallelize(map.values.toSeq)
    
    val linkDS = Seq( Link(1L, map.getOrElse("C", null).asInstanceOf[Point], map.getOrElse("A", null).asInstanceOf[Point] , 10.0, 40, 2, 2), 
                      Link(2L, map.getOrElse("E", null).asInstanceOf[Point], map.getOrElse("C", null).asInstanceOf[Point] , 5.0, 40, 2, 2), 
                      Link(3L, map.getOrElse("G", null).asInstanceOf[Point], map.getOrElse("E", null).asInstanceOf[Point] , 8.0, 40, 2, 2), 
                      Link(4L, map.getOrElse("I", null).asInstanceOf[Point], map.getOrElse("G", null).asInstanceOf[Point] , 4.0, 40, 2, 2), 
                      Link(5L, map.getOrElse("J", null).asInstanceOf[Point], map.getOrElse("I", null).asInstanceOf[Point] , 1.0, 40, 2, 2),
                      Link(6L, map.getOrElse("A", null).asInstanceOf[Point], map.getOrElse("J", null).asInstanceOf[Point] , 5.0, 40, 2, 2), 
                      Link(7L, map.getOrElse("J", null).asInstanceOf[Point], map.getOrElse("D", null).asInstanceOf[Point] , 8.0, 40, 2, 2))
    val linkRDD = sc.parallelize(linkDS).flatMap(link => {
     if(link.getDrivingDirection() == 1){
        List(link)
      }else{
        List(
          Link(link.getId(), link.getHead(), link.getTail(), link.getDistance(), link.getSpeed(), 1, link.getLanes()/2),
          Link(link.getId(), link.getTail(), link.getHead(), link.getDistance(), link.getSpeed(), 1, link.getLanes()/2)
        )
      }
   })
   
    val nodesRDD = nodeRDD.map(node => (node.getUserData.asInstanceOf[Long], node))
    val edgesRDD = linkRDD.map(link => {
            Edge(link.getTail().getUserData.asInstanceOf[Long],
                link.getHead().getUserData.asInstanceOf[Long], 
                link)
        })
        
    val graph = Graph(nodesRDD, edgesRDD)
    val A = map.getOrElse("A", null).asInstanceOf[Point]
    val G = map.getOrElse("G", null).asInstanceOf[Point]
    runDijkstra(graph, A.getUserData.asInstanceOf[Long], G.getUserData.asInstanceOf[Long])
    println("finished")
  }
  
  def runDijkstra(graph : Graph[Point, Link],  source : Long , destination : Long) : Unit = {
    val initialMessage : (Double, List[Point]) = (Double.MaxValue, List())
    val spGraph = graph.mapVertices { (vid, vertex) => if(vid == source) (0.0, List(vertex)) else (Double.MaxValue, List())}
    
    def vertexProgram(id: Long, vertex: (Double, List[Point]), msg: (Double, List[Point])): (Double, List[Point]) = {
      if(msg._1 == Double.MaxValue){
        vertex
      }else{
        if(vertex._1 < msg._1){
          vertex
        }else{
          msg
        }
      }
    }
    
    def sendMessage(triplet: EdgeTriplet[(Double, List[Point]), Link]): Iterator[(Long, (Double, List[Point]))] = {
      val source = triplet.srcAttr
      val destination = triplet.dstAttr
      val weight = triplet.attr.distance
      val cost = source._1 + weight
      if(cost > destination._1 || source._1 == Double.MaxValue){
        Iterator.empty
      }else{
        var list = source._2
        list = list :+ triplet.attr.getHead()
        Iterator((triplet.dstId, (cost, list)))
      }
    }
    
    def messageCombiner(msg1 : (Double, List[Point]), msg2: (Double, List[Point])) : (Double, List[Point]) = {
      if(msg1._1 < msg2._1) msg1 else msg2
    }
                      

    val pregel = Pregel(spGraph, initialMessage, Integer.MAX_VALUE)(vertexProgram, sendMessage, messageCombiner)
  }
  
  def runBuiltin(graph: Graph[Point, Link], sourceId: Long, destinationId: Long) = {
        
        val spResult = ShortestPaths.run(graph, Seq(sourceId, destinationId))
        val verticesRDD = spResult.vertices
        val verteicesRowRDD = verticesRDD.map(map => {
            Row.fromSeq(Seq(map._1, map._2.mkString(" | ")))
        })
        
        verteicesRowRDD.take(10).foreach(println)
        println("finished")
    }
}