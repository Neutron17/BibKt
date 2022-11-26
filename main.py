import dask.dataframe as dd

df = dd.read_csv('Chapters.csv', usecols=['BookID', 'Chapter', 'TotalVerses'])
df.to_csv('output.csv', index=False)