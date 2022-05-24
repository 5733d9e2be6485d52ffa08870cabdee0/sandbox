date_string=$(sed '1!d' ./.github/data/sheriff-order)
start_date=$(date --date=$date_string)
count=1
total_records=$(wc -l <./.github/data/sheriff-order)
let "total_records--"
while [ $(date --date="$start_date" +%s) -lt $(date +%s) ]; do
  DOW=$(date -d "$start_date" +%u)
  if [ \( $DOW -eq 4 \) -o \( $DOW -eq 5 \) ]; then
    start_date=$(date -d "$start_date+4days")
  else
    start_date=$(date -d "$start_date+2days")
  fi
  if [ $count -gt $total_records ]; then
    count=1
  fi
  let "count++"

done

head -$count ./.github/data/sheriff-order | tail -1
