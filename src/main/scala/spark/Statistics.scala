package spark

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

class Statistics {
  val df = Read.getListDataFrame()

  /**
   * 두 컬럼 사이의 영향도 비교
   * 공분산(covariance)
   *  - 표본공분산(sample covariance)
   *  - 모공분산(population covariance)
   * 상관 계수(Correlation coefficient) : 두 값의 관계를 알고 싶을 때 사용.
   */
  def correlationCoefficient() = {


    //갯수와 가격과의 상관 관계를 알고 싶을 때
    df.stat.corr("quantity", "unitPrice")
    df.select(corr("quantity", "unitPrice"))
    df.select(covar_pop("quantity", "unitPrice"))
    df.select(covar_samp("quantity", "unitPrice"))
  }

  def describe() = {
    df.describe("id", "uniform", "normal").show()
    /*
+-------+------------------+-------------------+--------------------+
|summary|                id|            uniform|              normal|
+-------+------------------+-------------------+--------------------+
|  count|                10|                 10|                  10|
|   mean|               4.5| 0.5215336029384192|-0.01309370117407197|
| stddev|2.8722813232690143|  0.229328162820653|  0.5756058014772729|
|    min|                 0|0.19657711634539565| -0.7195024130068081|
|    max|                 9| 0.9970412477032209|  1.0900096472044518|
+-------+------------------+-------------------+--------------------+
     */
  }

  def summary(spark: SparkSession) = {
    Read.createDataFrameByRow(spark)
      .summary().show()
    /*
+-------+------------------+------------------+-------+
|summary|                aa|                bb|     bb|
+-------+------------------+------------------+-------+
|  count|                 3|                 3|      3|
|   mean|2.3333333333333335|3.3333333333333335|   null|
| stddev|1.5275252316519468|1.5275252316519468|   null|
|    min|                 1|                 2|string1|
|    25%|                 1|                 2|   null|
|    50%|                 2|                 3|   null|
|    75%|                 4|                 5|   null|
|    max|                 4|                 5|string3|
+-------+------------------+------------------+-------+

     */
  }

  /**
   * There is two type of standard deviation.
   * - 표본표준편차(sample standard deviation)
   * - 모표준편차(population standard deviation)
   * todo check what is the difference
   */
  def standardDeviation() = {
    df.select(var_pop("dd"), var_samp("dd"))
      .select(stddev_pop("dd"), stddev_samp("dd"))
      .select(stddev("dd"), variance("dd")) //use sample standard deviation
  }

  /**
   * 비대칭도(skewness) : 데이터 평균의 비대칭 정도 측정
   * 첨도(kurtosis) : 데이터 끝 부분 측정
   *
   * 확률변수(random variable)의 환률분포(probability distribution)로 데이터 모델링할 때 특히 중요
   */
  def skewnessKurtosis() = {
    df.select(skewness("dd"), kurtosis("dd"))
  }

  def approxQuantile() = {
    df.stat.approxQuantile("value", Array(0.25, 0.5, 0.75), 0.1)
    //    Seq(3,1,5).toDF() => 1, 3, 5 해당 값의 중위값, 25% 의 값. 등을 구하는 것, relativeError는 오차 허용 값.
    //만약 quantile의 값에 해당 하는 row를 찾고 싶다면, 찾은 quantile 의 값으로 필터링해서, 다시 query하면 됨
  }

  def crosstab() = {
    //convert 'name' as row, 'item' as column
    df.stat.crosstab("name", "item").show()

    /**
     * +---------+----+-----+------+------+-------+
     * |name_item|milk|bread|apples|butter|oranges|
     * +---------+----+-----+------+------+-------+
     * |      Bob|   6|    7|     7|     6|      7|
     * |     Mike|   7|    6|     7|     7|      6|
     * |    Alice|   7|    7|     6|     7|      7|
     * +---------+----+-----+------+------+-------+
     */
  }
}