#!/bin/bash

if [[ 5 -ne $# ]]; then
    echo "import_candle.sh <currency pair> <time frame> <from date> <to date> <truncate>"
    exit 1
fi

currency_pair=$1
time_frame=$2
from_date=$3
to_date=$4
truncate=$5

curl -i -X POST "http://localhost:8080/api/v1/candle/import" \
     -H 'Content-Type: application/json' \
     --data @- <<EOF
{
  "currencyPair": "${currency_pair}",
  "timeFrame": "${time_frame}",
  "fromDate": "${from_date}",
  "toDate": "${to_date}",
  "truncate": ${truncate}
}
EOF