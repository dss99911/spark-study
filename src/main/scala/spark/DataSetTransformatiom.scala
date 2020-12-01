package spark

import org.apache.spark.sql.{Dataset, Encoder, Encoders}

//todo merge with DataFrames example
class DataSetTransformatiom {
  val (spark, text) = Read.getTextDataSet()
  import spark.implicits._
  text.first()
  text.count()
  text.filter((str: String) => str.contains("aa"))
  text.map(line => line.split(" ").size)
    .reduce((a, b) => Math.max(a, b))


  val wordCount = text.flatMap(line => line.split(" "))
    .groupByKey(s => identity(s)).count()
  wordCount.collect()//make Dataset to array

  /**
   * without `persist()` method, the data is not saved in memory
   * so, if you want to use the same data later, use `persist()`
   */
  def pergist() = {
    val uppercaseText = text.map(t => t.toUpperCase())
    uppercaseText.persist()
    uppercaseText.cache()//cache() is same with persist(). but cache() is only in memory. but persist(level) can set where to save.

    // persist data set is automatically cleared. but it takes time(least-recently-used (LRU) fashion)
    // so, If you want to clear directly after it's finished to use. then call 'unpersist()\'
    uppercaseText.unpersist()
  }
}