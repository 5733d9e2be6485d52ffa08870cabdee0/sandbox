MANAGER=$(git log -1 --format='%ci' 434cdfbff429d569c0813522c76a06c838915819 | date --utc)
SHARD=$(git log -1 --format='%ci' 365562bc0cac08b794c1aac0e207b85540b31051 | date --utc)
EXECUTOR=$(git log -1 --format='%ci' 365562bc0cac08b794c1aac0e207b85540b31051 | date --utc)

if [[ $MANAGER > $SHARD ]]
then
	if [[ $MANAGER > $EXECUTOR ]]
	then
		TO_USE="MANAGER"
	else
		TO_USE="EXECUTOR"
	fi
elif [[ $SHARD > $EXECUTOR ]]
then 
	TO_USE="SHARD"
else
	TO_USE="EXECUTOR"
fi

printf $TO_USE
