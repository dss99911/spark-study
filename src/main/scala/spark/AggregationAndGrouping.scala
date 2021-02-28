package spark

import org.apache.spark.sql.functions.{approx_count_distinct, avg, col, collect_list, collect_set, count, countDistinct, expr, first, grouping, grouping_id, last, max, min, struct, sum, sumDistinct, udf}
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import spark.Read.Person

import scala.collection.mutable

class AggregationAndGrouping {
  private val spark: SparkSession = SparkSessions.createSparkSession()
  import spark.implicits._
  private val df: DataFrame = Read.getParquetDataFrame()

  def onSelect() = {
    df.select(count($"code"))//if code is null then it's not counted. if use '*' then count all rows
      .select(countDistinct("code"))//select count(distinct code) from some_table
      .select(approx_count_distinct("code", 0.1))//대략적인 count. 데이터가 클 경우, 정확하지 않지만, 빠르게 근사치를 얻고 싶을 때
      .select(first("code"))
      .select(last("code"))
      .select(min("code"))
      .select(max("code"))
      .select(sumDistinct("code"))
      .select(avg("code"))
      .select(sum('count), expr("avg(count)"), expr("count(distinct(age))"))//able to use aggregation function
  }

  def withAgg() = {
    df.agg(collect_list("value").as("list"))
    df.agg(collect_set("value").as("set"))
    df.agg(count("value").as("count"), expr("count(value)").as("count2"))
    df.agg("value" -> "avg", "value" -> "count")//column -> agg method name

  }

  def makeListPerGroup = {
    //agg to list and map. change it to data set
    val numDF = (1 to 100).toDF()
      .withColumn("ten", ($"value" / 10).cast(IntegerType))
    numDF
      .groupBy("ten")
      .agg(collect_list("value").as("list"))
      .agg(collect_list(struct(numDF.columns.map(col):_*)).as("all_list"))//make all column as list
      .map(r => r.getAs[List[Int]]("list").sum)
      .show(100)

    //the approach above use driver program, the below handle list on distributed processing
    //we have to consider partition as well. group by object's field may not be efficient. because it may cause full shuffle
    def someFunction(name: String, persons: Iterator[Person]) = {
      "???"
    }

    df.as[Person]
      .groupByKey(_.name)
      .mapGroups((name: String, persons: Iterator[Person]) => {
        someFunction(name, persons)
      })
    df.as[Person]
      .groupByKey(_.name)
      .mapGroups(someFunction)//able to use function name directly.

    df.repartition($"name")
      .mapPartitions((rows: Iterator[Row]) =>
        rows.map(_.getAs[String](0))
      )

    //agg to list and convert list by udf.
    // - able to keep Dataframe
    val updateList = udf((list: mutable.WrappedArray[Int]) => list.sum)
    (1 to 100).toDF()
      .withColumn("ten", ($"value"/10).cast(IntegerType))
      .groupBy("ten")
      .agg(collect_list("value").as("list"))
      .withColumn("list", updateList($"list"))
      .show(100)
  }

  def group() = {
    df.groupBy("age").count()// (age, count), count()'s column name is 'count'
  }

  /**
   * grouping set
   * - https://www.sqlservertutorial.net/sql-server-basics/sql-server-grouping-sets/
   */
  def groupingSet() = {
    //this is supported by sql only
    val dfNoNull = df.na.drop()
    dfNoNull.createOrReplaceTempView("dfNoNull")
    spark.sql("select customerId, stockCode, sum(quantity) from dfNoNull\n" +
      "group by customerId, stockCode grouping sets((customerId, stockCode), (stockCode), ())\n" +
      "order by sum(quantity) desc, customerId desc, stockCode desc")
  }

  /**
   * this is for grouping set
   */
  def rollUp() = {
    val dfNoNull = df.na.drop()
    dfNoNull
      .rollup("date", "country")
      .agg(sum("quantity"))
      .selectExpr("date", "country", "`sum(quantity)` as total_quantity")//sum의 이름을 바꿀려고, select문을 쓴 것 같은데..??
      .orderBy("date")
      .show()
    //date+country별, date별, 전체에 대한 총양을 구하기
  }

  def cube() = {
    val dfNoNull = df.na.drop()
    dfNoNull
      .cube("date", "country")
      .agg(sum("quantity"))
      .agg(grouping_id(), sum("quantity"))//show grouping Id.
      .agg(grouping("date"), sum("quantity"))//aggregated or not.
      .orderBy("date")
      .show()
    //date+country별, date별, country별, 전체에 대한 총양을 구하기
  }

  /**
   * cube와 rollup의 차이 : https://www.mikulskibartosz.name/cube-and-rollup-in-apache-spark/#:~:text=Both%20functions%20are%20used%20to,from%20the%20first%20given%20column.
CUBE	GROUP BY
year, month, day	SELECT COUNT(*) FROM table GROUP BY year, month, day
year, month	SELECT COUNT(*) FROM table GROUP BY year, month
year, day	SELECT COUNT(*) FROM table GROUP BY year, day
year	SELECT COUNT(*) FROM table GROUP BY year
month, day	SELECT COUNT(*) FROM table GROUP BY month, day
month	SELECT COUNT(*) FROM table GROUP BY month
day	SELECT COUNT(*) FROM table GROUP BY day
null, null, null	SELECT COUNT(*) FROM table
If I used the rollup function, the grouping would look like this:

ROLLUP	GROUP BY
year, month, day	SELECT COUNT(*) FROM table GROUP BY year, month, day
year, month	SELECT COUNT(*) FROM table GROUP BY year, month
year	SELECT COUNT(*) FROM table GROUP BY year
null	SELECT COUNT(*) FROM table
   */

  /**
   * rows를 columns로 변환
   */
  def pivot() = {
    df.groupBy("date").pivot("country").sum()
    //날짜별, 나라별, 합계 테이블을 만든다
  }


  /**
   * UDAF : User defined aggregation function
   * 사용자 정의 집계 함수
   */
}