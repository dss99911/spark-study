package spark

import org.apache.spark.sql.functions.col

class WriteParquet {
  ReadParquet.getDataFrame().selectExpr("aid", "iid", "sdr", "msg", "mrt")
    .write
    .partitionBy("dt")
    .mode("overwrite")
    .parquet(s"s3://key")
}
