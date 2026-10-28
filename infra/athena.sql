-- Replace the bucket and prefix. Upload Parquet files into event_date=YYYY-MM-DD/.
CREATE EXTERNAL TABLE IF NOT EXISTS analytics.transactions (
  transaction_id string,
  amount decimal(18,2)
)
PARTITIONED BY (event_date string)
STORED AS PARQUET
LOCATION 's3://YOUR-CURATED-BUCKET/transactions/';

-- For this small setup, discover existing partitions. Use Glue crawlers,
-- explicit partition registration or projection for a production data lake.
MSCK REPAIR TABLE analytics.transactions;
