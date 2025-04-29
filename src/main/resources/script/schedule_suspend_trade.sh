#!/bin/bash

if [[ 1 -ne $# ]]; then
    echo "schedule_suspend_trade.sh <suspend datetime(yyyy-MM-ddTHH:mm:ss)>"
    exit 1
fi

suspend_datetime=$1

curl -X POST "http://localhost:8080/api/v1/trade/suspend/schedule" \
  -H 'Content-Type: application/json' \
  --data @- <<EOF
{
  "time": "${suspend_datetime}"
}
EOF
