from datetime import date, datetime

import numpy as np
import pandas as pd

df = pd.DataFrame({
    'a': [1, 2, 3],
    'b': [2., 3., 4.],
    'c': ['string1', 'string2', 'string3'],
    'd': [date(2000, 1, 1), date(2000, 2, 1), date(2000, 3, 1)],
    'e': [datetime(2000, 1, 1, 12, 0), datetime(2000, 1, 2, 12, 0), datetime(2000, 1, 3, 12, 0)]
})
dates = pd.date_range("20130101", periods=6)
df_number = pd.DataFrame(np.random.randn(6, 4), index=dates, columns=list("ABCD"))
s = pd.Series([1, 3, 5, np.nan, 6, 8], index=dates).shift(2)
# %% Filter
df_head = df.head()  # first 5 rows
df_tail = df.tail(3)  # last 3 rows

# %%

s_lower = df['c'].str.lower()
df_subtract = df_number.sub(s, axis='index')  # subtraction


df_transpose = df.T  # convert column and row
df_copy = df.copy()
df_shift = df_number.shift(2)  # shift values down (last rows removed. first rows are nan

# apply는 리턴값이 series인지, 단일 값인지에 따라서, 리턴값으로 dataframe 또는 series를 리턴한다.
df_cumsum = df_number.apply(np.cumsum)  # cumulative sum. new-row(n) = new-row(n-1) + existing-row(n)
s_filter_by_row = df.apply(lambda r: r['a'] > 1, axis='columns')  # 각 index 별, a column을 체크한다.
s_max_min_by_column = df_number.apply(lambda x: x.max() - x.min())  # 각 column별, min, max

#날짜 텍스트를 날짜로 변환
df_datetime_text = pd.read_csv('data/SP500_NOV2019_Hist.csv')
df_datetime = pd.to_datetime(df_datetime_text['Date'])

# 전체 데이터 타입변환
df_float = df.astype(float)

# %% When Otherwise
df['a_sign'] = np.where(df['a'] > 1, 'plus', 'minus')
replace_fct = {1: "A", 2: 'B', 3: 'C'}
df['a'] = df['a'].map(replace_fct)