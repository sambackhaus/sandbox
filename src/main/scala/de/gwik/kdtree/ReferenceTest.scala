package de.gwik.kdtree

import java.util.UUID

import scala.util.Random

object ReferenceTest extends AbsTest {

  trait Scorable {
    def id: String

    def veryShortPrefix: String
  }

  case class Scored[T](item: T, score: Double, model: String) {

    def applyFactor(scoreFactor: Int) = Scored(item, score * scoreFactor, model)

    def add(additional: Double) = Scored(item, score + additional, model)
  }

  def dotProduct(a: Array[Double], b: Array[Double]): Double = {
    val length = a.length
    var i = 0
    var dot = 0.0
    while (i < length) {
      dot += a(i) * b(i)
      i += 1
    }
    dot
  }

  def toScore(articleIAndVector: (String, Array[Double]),
              userVector: Array[Double]): Scored[String] = {
    val score = dotProduct(userVector, articleIAndVector._2)
    Scored[String](articleIAndVector._1, score, "tsl_ppe_lt")
  }

  def createRandomVector(dim : Int) : Array[Double] = {
    (for (j <- 1 to dim) yield Random.nextDouble()).toArray
  }

  override def main(args: Array[String]): Unit = {
    var start = System.currentTimeMillis()
    print(s"$start: creating $numPoints test sequences with $dimensions dimensions...")
    val testSequences : IndexedSeq[(String, Array[Double])] = for (i <- 1 to numPoints) yield (UUID.randomUUID().toString -> createRandomVector(dimensions))
    println(s"done (took: ${System.currentTimeMillis()-start}ms)")

    start = System.currentTimeMillis()
    print(s"$start: creating $samples random test sequences...")
    val rndTestSeq = for (i <- numPoints*dimensions +1 to numPoints*dimensions + samples) yield createRandomVector(dimensions)
    println(s"done (took: ${System.currentTimeMillis()-start}ms)")

    val allStart = System.currentTimeMillis()

    val deltaTs = rndTestSeq.map(i => {
      start = System.currentTimeMillis()
      print(s"$start: looking for neighbours...")

      val topScores = collection.mutable.PriorityQueue()(Ordering.by[Scored[String], Double](-_.score))
      var highestDropout = Double.NaN
      testSequences.foreach { prod =>
        val scoredArticle = toScore(prod, i)
        if (highestDropout.isNaN || scoredArticle.score > highestDropout) {
          topScores.enqueue(scoredArticle)
          if (topScores.length > neighbours) highestDropout = topScores.dequeue().score
        }
      }
      topScores.dequeueAll.reverse
      val deltaT = System.currentTimeMillis()-start
      println(s"done (took: ${deltaT}ms)")
      deltaT
    })
    println(s"average: ${deltaTs.sum.toDouble/samples.toDouble}ms")

    println(s"Found $samples * $neighbours in ${System.currentTimeMillis()-allStart}ms")

    print("fin!")

  }

}