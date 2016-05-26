#!/bin/bash -e

# execute within a TMUX session
if [ -z $TMUX ]; then
	echo "execute within a tmux session (e.g. tmux -new -s lms)"
	exit 1
fi

# edu
evalfile_f=~/data/lm/kdslbot-focused-edu-08/splits/stdin_sorted_2.txt
evalfile_nf=~/data/lm/kdslbot-non-focused-edu-10/splits/stdin_sorted_2.txt
resultfile_f=~/eval/2015-03-02/docperp_f
resultfile_nf=~/eval/2015-03-02/docperp_nf

trainlm=~/data/lm/kdslbot-focused-edu-08/corpora_split_log_numtokens_train/0_m1
#trainlm=~/data/lm/kdslbot-focused-edu-08/corpora_split_log_numtokens_train_de/0_m1

# start lm
wname="docperp"
id="trainlm"
dir=${trainlm}
tmux new-window -n ${wname} -d \
"lm.sh \
-Dlt.lm.tokenfilter=6 \
-Dlt.lm.tokennormalize=1 \
-Dlt.lm.knUnkLog10Prob=NaN \
-Dlt.lm.insertSentenceTags=1 \
-Dlt.lm.handleBoundaries=2 \
-Dlogfile=${HOME}/logs/${id}.log \
de.tudarmstadt.lt.lm.app.StartLM \
-pt LtSegProvider \
-t KneserNeyLM \
-n 5 -m 1 -i ${id} -d ${dir} -rp 10991; exec bash"

#-t BerkeleyLM \
#-Dlt.lm.tokenizer=de.tudarmstadt.lt.seg.token.EmptySpaceTokenizer \
#-Dlt.lm.sentenceSplitter=de.tudarmstadt.lt.seg.sentence.LineSplitter \

while true
do
	# wait until train lm is runnning
	sleep 5
	r1=$(lm.sh de.tudarmstadt.lt.lm.app.ListServices -p 10991 | grep running | cut -f1 | grep ^${id}$ | wc -l)
	if [[ $r1 == 1 ]]
	then

		# PerpDoc focused
		tmux split-window -t :${wname} -d \
		"~/bin/lm.sh de.tudarmstadt.lt.lm.app.PerpDoc -p 10991 -i ${id} -q -f ${evalfile_f} -o ${resultfile_f}_woov.txt; exec bash"
		tmux select-layout -t :${wname} tiled
		sleep 2

		tmux split-window -t :${wname} -d \
		"~/bin/lm.sh de.tudarmstadt.lt.lm.app.PerpDoc -p 10991 --noov true -i ${id} -q -f ${evalfile_f} -o ${resultfile_f}_noov.txt; exec bash"
		tmux select-layout -t :${wname} tiled
		sleep 2

		# PerpDoc non-focused
		tmux split-window -t :${wname} -d \
		"~/bin/lm.sh de.tudarmstadt.lt.lm.app.PerpDoc -p 10991 -i ${id} -q -f ${evalfile_nf} -o ${resultfile_nf}_woov.txt; exec bash"
		tmux select-layout -t :${wname} tiled
		sleep 2

		tmux split-window -t :${wname} -d \
		"~/bin/lm.sh de.tudarmstadt.lt.lm.app.PerpDoc -p 10991 --noov true -i ${id} -q -f ${evalfile_nf} -o ${resultfile_nf}_noov.txt; exec bash"
		tmux select-layout -t :${wname} tiled
		
		break

	fi
done
