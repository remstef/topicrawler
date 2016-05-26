#!/bin/bash -e

# execute within a TMUX session
if [ -z $TMUX ]; then
	echo "execute within a tmux session (e.g. tmux -new -s lms)"
	exit 1
fi

# edu
#evalfile=~/data/lm/pedocs-test/pedocs.test.sentences.1000.txt
evalfile=~/data/lm/pedocs-test/pedocs.test.sentences_de_tok_1K_ngrams
#evalfile=~/data/lm/pedocs-test/pedocs.test.sentences_tok_1K_sboundngrams
resultfile=~/eval/2015-02-26/lmeval_1K_de_oovref
fbasedir=~/data/lm/kdslbot-focused-edu-08/corpora_split_log_numtokens_train_de
nfbasedir=~/data/lm/kdslbot-non-focused-edu-10/corpora_split_log_numtokens_train_de

flmdirs=$(ls ${fbasedir}/* -d | grep "_m1" | grep  "[0-3]00M")
nflmdirs=$(ls ${nfbasedir}/* -d | grep "_m1" | grep  "[0-3]00M")
oovreflmid="f5_0_m1"

# amh
#evalfile=~/data/amharic/data/test/train_sent_tok_shuf_test
#resultfile=~/eval/2015-02-24/lmeval_amh_oovref_t
#fbasedir=~/data/lm/kdslbot-focused-am-01/corpora_split_log_numtokens_train
#nfbasedir=~/data/lm/kdslbot-non-focused-am-01/corpora_split_log_numtokens_train

#flmdirs=$(ls ${fbasedir}/* -d | grep "_m1")
#nflmdirs=$(ls ${nfbasedir}/* -d | grep "_m1")
#oovreflmid="f5_0_m1"

for a in $flmdirs; do echo $a; done
for a in $nflmdirs; do echo $a; done
sleep 3

cardinalities=(5)

# define some helper functions
function evallm(){
	n=$1
	dir=$2
	prefix=$3
	# start lm
	wname="lm${n}"
	id="${prefix}_$(basename ${dir})"

	tmux split-window -t :${wname} -d \
"lm.sh \
-Dlt.lm.tokenizer=de.tudarmstadt.lt.seg.token.EmptySpaceTokenizer \
-Dlt.lm.sentenceSplitter=de.tudarmstadt.lt.seg.sentence.LineSplitter \
-Dlt.lm.tokenfilter=6 \
-Dlt.lm.tokennormalize=1 \
-Dlt.lm.knUnkLog10Prob=NaN \
-Dlt.lm.insertSentenceTags=1 \
-Dlt.lm.handleBoundaries=2 \
-Dlogfile=${HOME}/logs/${id}.log \
de.tudarmstadt.lt.lm.app.StartLM \
-pt LtSegProvider \
-t KneserNeyLM \
-n ${n} -m 1 -i ${id} -d ${dir} -rp 10990; exec bash"

	tmux select-layout -t :${wname} tiled

	while true
	do
		# wait until lm is runnning and oovreflm is running
		sleep 5
		r1=$(lm.sh de.tudarmstadt.lt.lm.app.ListServices -p 10990 | grep running | cut -f1 | grep ^${id}$ | wc -l)
		r2=$(lm.sh de.tudarmstadt.lt.lm.app.ListServices -p 10990 | grep running | cut -f1 | grep ^${oovreflmid}$ | wc -l)
		if [[ $r1 == 1 && $r2 == 1 ]]
		then
			# PerplexityClient SentPerp
			~/bin/lm.sh de.tudarmstadt.lt.lm.app.PerplexityClient --one_ngram_per_line -p 10990 -i ${id} -q -f ${evalfile} -o ${resultfile}"_woov.txt" --oovreflm ${oovreflmid}
			~/bin/lm.sh de.tudarmstadt.lt.lm.app.PerplexityClient --one_ngram_per_line -p 10990 --noov true -i ${id} -q -f ${evalfile} -o ${resultfile}"_noov.txt" --oovreflm ${oovreflmid}
			# kill the lm
			#pids=$(pgrep -lf ${id} | cut -f1 -d' ')
			#kill $pids
			break
		fi
	done
}

# start new window containg rmi server
wname="lm${cardinalities[0]}"
tmux new-window -n ${wname} -d "lm.sh de.tudarmstadt.lt.lm.app.StartRMI -p 10990 ; exec bash;"

sleep 2

for c in ${cardinalities[@]};
do
	if [[ $c != ${cardinalities[0]} ]]; then
		wname="lm${c}"
		tmux new-window -n ${wname}
	fi

	echo $c
	# evaluate each lm (focused)
	for flm in ${flmdirs}
	do
		evallm $c $flm f${c} &
		sleep 2
	done

	# evaluate each lm (non-focused)
	for nflm in ${nflmdirs}
	do
		evallm $c $nflm nf${c} &
		sleep 2
	done

	#sleep 7200
	sleep 100

done
