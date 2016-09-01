#!/bin/bash -e

# execute within a TMUX session
if [ -z $TMUX ]; then
	echo "execute within a tmux session (e.g. tmux -new -s lms)"
	exit 1
fi

# define some helper functions
function evallm(){
	t=$1
	m=$2
	s=$3
	oovreflmid=$4
	
	# start lm
	lmid=$t-$m-$s

	tmux split-window -t :$wname -d "lm-nightly -Xmx10g -Dlogfile=$HOME/logs/$lmid-lm.log -Dlt.lm.knUnkLog10Prob=-10 de.tudarmstadt.lt.lm.app.StartLM -t BerkeleyLM -n 3 -d $b/$t-$m/$s -i $lmid"
	tmux select-layout -t :$wname tiled

	while true
	do
		# wait until lm is runnning and oovreflm is running
		sleep 5
		r1=$(lm-nightly de.tudarmstadt.lt.lm.app.ListServices 2> /dev/null | grep running | cut -f1 | grep ^${lmid}$ | wc -l)
		r2=$(lm-nightly de.tudarmstadt.lt.lm.app.ListServices 2> /dev/null | grep running | cut -f1 | grep ^${oovreflmid}$ | wc -l)
		if [[ $r1 == 1 && $r2 == 1 ]]
		then
			# PerplexityClient
			lm-nightly -Dlogfile=$HOME/logs/$lmid-pc.log -Dverbosity=WARN de.tudarmstadt.lt.lm.app.PerplexityClient --one_ngram_per_line -i $lmid -q -f $b/$t/test-ngrams-r250.txt --oovreflm $oovreflmid --skipoov true --skipoovreflm true 1>> $r & p1=$!
			sleep 2
			lm-nightly -Dlogfile=$HOME/logs/$lmid-pc.log -Dverbosity=WARN de.tudarmstadt.lt.lm.app.PerplexityClient --one_ngram_per_line -i $lmid -q -f $b/$t/test-ngrams-r250.txt --oovreflm $oovreflmid --skipoov false --skipoovreflm false 1>> $r & p2=$!
			# kill the lm
			wait $p1
			wait $p2
			pgrep -lf ${lmid} | cut -f1 -d' ' | xargs kill
			break
		fi
	done
}

b=$HOME/data/texeval-2015
r=$b/eval/eval.tsv
topics="ai-en vehicles-en plants-en"
models="nf 1 2 3 5"
sizes="0 100K 300K 1M 3M 10M 30M 100M"

# start new window containing rmi server
wname="lm-eval"
tmux new-window -n $wname -d "bash"
tmux split-window -t :$wname -d "lm-nightly de.tudarmstadt.lt.lm.app.StartRMI"

sleep 2

for t in $topics; do 
	# start training lm for oov reference
	d=$b/$t/lm-small
	tmux split-window -t :$wname -d "lm-nightly -Xmx10g -Dlt.lm.knUnkLog10Prob=-10 de.tudarmstadt.lt.lm.app.StartLM -t BerkeleyLM -n 5 -d $d -i ${t}-train"
	sleep 2
	for m in $models; do 
		for s in $sizes; do
			echo $t $m $s
			evallm $t $m $s "${t}-train" &
			sleep 2 
		done
		wait
	done
	# kill the training lm
	pgrep -lf ${t}-train | cut -f1 -d' ' | xargs kill
	sleep 2
done