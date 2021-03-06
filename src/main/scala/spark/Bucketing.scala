package spark

import org.apache.spark.sql.SparkSession

/**
 * https://luminousmen.com/post/the-5-minute-guide-to-using-bucketing-in-pyspark
 * https://jaceklaskowski.gitbooks.io/mastering-spark-sql/content/spark-sql-bucketing.html
 * https://mathjhshin.github.io/Spark%EC%97%90%EC%84%9C%EC%9D%98-Bucketing/
 *
 * partitioning을 unlimited한 필드에 하게 되면, partition이 너무 많이 발생하게 되고, 이게 오히려, 성능저하를 야기할 수 있다.
 * 그래서, bucketing을 통해, 일정한 크기만큼만 분할학 위한 목적으로 이해했었지만
 *
 * join시 특정 shuffle을 방지하는 목적이라고 함..
 *
 * shuffle 방지 목적
 *
 * 사용 예
 * - id로 조인하는 경우, id로 partition을 나누면 파티션이 너무 많이 만들어지고, 파티션별로 폴더가 생성된다.
 * - 이 경우, id로 bucketing하면, sortMergeJoin등에서 shuffle이 일어나지 않는다.
 *
 */
class Bucketing {
  private val spark: SparkSession = SparkSessions.createSparkSession()
  val t1 = spark.table("unbucketed1")
  val t2 = spark.table("unbucketed2")

  t1.join(t2, "key").explain()
  /*
  this shows Exchange hashpartitioning. because "key" is not partitioned. so need shuffle
  == Physical Plan ==
*(5) Project [key#10L, value#11, value#15]
+- *(5) SortMergeJoin [key#10L], [key#14L], Inner
   :- *(2) Sort [key#10L ASC NULLS FIRST], false, 0
   :  +- Exchange hashpartitioning(key#10L, 200)
   :     +- *(1) Project [key#10L, value#11]
   :        +- *(1) Filter isnotnull(key#10L)
   :           +- *(1) FileScan parquet default.unbucketed1[key#10L,value#11] Batched: true, Format: Parquet, Location: InMemoryFileIndex[file:/opt/spark/spark-warehouse/unbucketed1], PartitionFilters: [], PushedFilters: [IsNotNull(key)], ReadSchema: struct<key:bigint,value:double>
   +- *(4) Sort [key#14L ASC NULLS FIRST], false, 0
      +- Exchange hashpartitioning(key#14L, 200)
         +- *(3) Project [key#14L, value#15]
            +- *(3) Filter isnotnull(key#14L)
               +- *(3) FileScan parquet default.unbucketed2[key#14L,value#15] Batched: true, Format: Parquet, Location: InMemoryFileIndex[file:/opt/spark/spark-warehouse/unbucketed2], PartitionFilters: [], PushedFilters: [IsNotNull(key)], ReadSchema: struct<key:bigint,value:double>, SelectedBucketsCount: 16 out of 16
   */

  t1.write
  .bucketBy(16, "key")
    .sortBy("value")
    .saveAsTable("bucketed")

  val t3 = spark.table("bucketed")
  val t4 = spark.table("bucketed")
  t3.join(t4, "key").explain()
  /*
  No exchange is shown
  == Physical Plan ==
*(3) Project [key#14L, value#15, value#30]
+- *(3) SortMergeJoin [key#14L], [key#29L], Inner
   :- *(1) Sort [key#14L ASC NULLS FIRST], false, 0
   :  +- *(1) Project [key#14L, value#15]
   :     +- *(1) Filter isnotnull(key#14L)
   :        +- *(1) FileScan parquet default.bucketed[key#14L,value#15] Batched: true, Format: Parquet, Location: InMemoryFileIndex[file:/opt/spark/spark-warehouse/bucketed], PartitionFilters: [], PushedFilters: [IsNotNull(key)], ReadSchema: struct<key:bigint,value:double>, SelectedBucketsCount: 16 out of 16
   +- *(2) Sort [key#29L ASC NULLS FIRST], false, 0
      +- *(2) Project [key#29L, value#30]
         +- *(2) Filter isnotnull(key#29L)
            +- *(2) FileScan parquet default.bucketed[key#29L,value#30] Batched: true, Format: Parquet, Location: InMemoryFileIndex[file:/opt/spark-warehouse/bucketed], PartitionFilters: [], PushedFilters: [IsNotNull(key)], ReadSchema: struct<key:bigint,value:double>, SelectedBucketsCount: 16 out of 16
   */
}
