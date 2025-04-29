#!/bin/bash

if [[ 1 -ne $# ]]; then
    echo "resume_trade.sh <resume datetime(yyyy-MM-ddTHH:mm:ss)>"
    exit 1
fi

resume_datetime=$1

curl -X POST "http://localhost:8080/api/v1/trade/resume/schedule" \
  -H 'Content-Type: application/json' \
  --data @- <<EOF
{
  "time": "${resume_datetime}"
}
EOF
