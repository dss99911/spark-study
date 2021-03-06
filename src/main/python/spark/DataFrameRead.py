from datetime import datetime, date
import pandas as pd
from pyspark.sql import Row
from pyspark.sql import SparkSession

spark = SparkSession.builder.getOrCreate()


def create_by_row():
    return spark.createDataFrame([
        Row(a=1, b=2., c='string1', d=date(2000, 1, 1), e=datetime(2000, 1, 1, 12, 0)),
        Row(a=2, b=3., c='string2', d=date(2000, 2, 1), e=datetime(2000, 1, 2, 12, 0)),
        Row(a=4, b=5., c='string3', d=date(2000, 3, 1), e=datetime(2000, 1, 3, 12, 0))
    ])


def create_by_schema():
    return spark.createDataFrame([
        (1, 2., 'string1', date(2000, 1, 1), datetime(2000, 1, 1, 12, 0)),
        (2, 3., 'string2', date(2000, 2, 1), datetime(2000, 1, 2, 12, 0)),
        (3, 4., 'string3', date(2000, 3, 1), datetime(2000, 1, 3, 12, 0))
    ], schema='a long, b double, c string, d date, e timestamp')


def create_from_pandas():
    """# https://spark.apache.org/docs/latest/api/python/user_guide/arrow_pandas.html"""
    pandas_df = pd.DataFrame({
        'a': [1, 2, 3],
        'b': [2., 3., 4.],
        'c': ['string1', 'string2', 'string3'],
        'd': [date(2000, 1, 1), date(2000, 2, 1), date(2000, 3, 1)],
        'e': [datetime(2000, 1, 1, 12, 0), datetime(2000, 1, 2, 12, 0), datetime(2000, 1, 3, 12, 0)]
    })
    return spark.createDataFrame(pandas_df)


# from RDD
def create_from_rdd():
    rdd = spark.sparkContext.parallelize([
        (1, 2., 'string1', date(2000, 1, 1), datetime(2000, 1, 1, 12, 0)),
        (2, 3., 'string2', date(2000, 2, 1), datetime(2000, 1, 2, 12, 0)),
        (3, 4., 'string3', date(2000, 3, 1), datetime(2000, 1, 3, 12, 0))
    ])
    return spark.createDataFrame(rdd, schema=['a', 'b', 'c', 'd', 'e'])


def read_from_excel(path):
    """pip install openpyxl for reading"""
    pandas_df = pd.read_excel(path, index_col=0)  # 인덱스 컬럼이 연속해서 추가되는 것을 방지, 인덱스 컬럼을 0번 컬럼으로
    return spark.createDataFrame(pandas_df)


if __name__ == '__main__':
    df = create_from_rdd()
    df.show()
    df.printSchema()
    read_from_excel('excel_test.xlsx').show()
