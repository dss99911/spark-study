package spark

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{dense_rank, rank, row_number, window}
import org.apache.spark.sql.types.TimestampType

/**
 * rowsBetween
 * - Window.unboundedPreceding : indicates that the window starts at the first row of the partition
 * - Window.currentRow : indicates the window begins or ends at the current row
 * - Window.unboundedFollowing : indicates that the window ends at the last row of the partition
 * -
 */
class Windows {
  val spark: SparkSession = SparkSessions.createSparkSession()
  import spark.implicits._

  private val df: DataFrame = Read.getParquetDataFrame()
  df.drop()
    df.withColumn("num",
      row_number.over(
        //window 범위 설정
        Window
          .partitionBy("pattern_id", "error_message")
          .orderBy("pattern_id", "error_message")

          //디폴트가 파티션 분할된 전체인지는 잘 모르겠지만, 생략해도 됨
          // 현재 row를 기준으로 +,- 범위 설정
          .rowsBetween(Window.unboundedPreceding, Window.currentRow)
      )
    )

  def window_functions() = {
    row_number()//frame내에서 순서(공통된 값이 있어도, 정렬된 순서에 따라 숫자가 정해짐
    rank()//공동1등이 있으면, 둘다 1이 되고, 그 다음은 3임.
    dense_rank()//공통1등이 있으면, 둘다 1이 되고, 그 다음은 2임.
  }

  def getFirstRowOfGroup() = {
    import org.apache.spark.sql.SaveMode
    import org.apache.spark.sql.expressions.Window

    val w = Window.partitionBy("type").orderBy("type")
    df.withColumn("rn", row_number.over(w))
      .where($"rn" === 1).drop("rn")
      .write
      .mode(SaveMode.Overwrite)
      .parquet("s3://hyun/temp/test")
  }

  def groupByDateRange() = {
    val a = Seq(System.currentTimeMillis() / 1000,2,3,4,5)

    a.toDF().withColumn("date", $"value".cast(TimestampType))
      .groupBy(window($"date", "1 day"))
      .count()

      .show(truncate = false)

    //    [1970-01-01 00:00:00, 1970-01-02 00:00:00]	4
    //    [2021-02-17 00:00:00, 2021-02-18 00:00:00]	1
  }
}