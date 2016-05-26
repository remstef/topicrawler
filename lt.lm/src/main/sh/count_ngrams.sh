#!/bin/bash
set -e

# generate ngrams for example by invoking
#
#  lm.sh -Dlt.lm.insertSentenceTags=1 de.tudarmstadt.lt.lm.app.Ngrams -a -n 1-5 -f ${lm_dir_or_file} -o ${raw_ngrams_file}
#
# equivalent:
# 
#  lm.sh -Dlt.lm.insertSentenceTags=1 de.tudarmstadt.lt.lm.app.GenerateNgramIndex -a -f target/test-classes/cat/ -n 1-5 -d target/test-classes/cat/indexdir -w
#

raw_ngrams_file=${1} # 'ngram.raw.txt.gz'
echo "${raw_ngrams_file}"
py_base=${2:-'.'} # dir of mr_*.py files
echo "${py_base}"
mincount=${3:-'1'} # filter counts below that value
echo "${mincount}"

ngrams_counted='ngram.counts.txt.gz'
echo "${ngrams_counted}"

ngrams_vocabulary='ngram.vocabulary.txt.gz'
echo "${ngrams_vocabulary}"

ngrams_follow_counted=${ngrams_counted%.txt.gz}'.nfollow.txt.gz'
echo "${ngrams_follow_counted}"

ngrams_precede_counted=${ngrams_counted%.txt.gz}'.nprecede.txt.gz'
echo "${ngrams_precede_counted}"

ngrams_followerprecede_counted=${ngrams_counted%.txt.gz}'.nfollowerprecede.txt.gz'
echo "${ngrams_followerprecede_counted}"

ngrams_joined_counts=${ngrams_counted%.txt.gz}'.joined.txt.gz'
echo "${ngrams_joined_counts}"

if [ -f ${raw_ngrams_file} ] && [ ! -f ${ngrams_counted} ]; then
	echo "creating ${ngrams_counted}"
	time (export LC_ALL=C; cat ${raw_ngrams_file} | gzip -c -d | python ${py_base}/mr_ngram_count.py -m | sort --parallel=8 | python ${py_base}/mr_ngram_count.py -r ${mincount}| gzip -c > ${ngrams_counted})
fi

if [ -f ${ngrams_counted} ] && [ ! -f ${ngrams_vocabulary} ]; then
	echo "creating ${ngrams_vocabulary}"
	time (export LC_ALL=C; cat ${ngrams_counted} | gzip -c -d | python ${py_base}/mr_ngram_vocab.py -m | sort --parallel=8 | python ${py_base}/mr_ngram_vocab.py -r | gzip -c > ${ngrams_vocabulary})
fi

if [ -f ${ngrams_counted} ] && [ ! -f ${ngrams_follow_counted} ]; then
	echo "creating ${ngrams_follow_counted}"
	time (export LC_ALL=C; cat ${ngrams_counted} | gzip -c -d | python ${py_base}/mr_ngram_nfollow.py -m | sort -k1,1 -t$'\t' --parallel=8 | python ${py_base}/mr_ngram_nfollow.py -r | gzip -c > ${ngrams_follow_counted}) # just sort is ok too
fi

if [ -f ${ngrams_counted} ] && [ ! -f ${ngrams_precede_counted} ]; then
	echo "creating ${ngrams_precede_counted}"
	time (export LC_ALL=C; cat ${ngrams_counted} | gzip -c -d | python ${py_base}/mr_ngram_nprecede.py -m | sort -k1,1 -t$'\t' --parallel=8 | python ${py_base}/mr_ngram_nprecede.py -r | gzip -c > ${ngrams_precede_counted}) # -k1,1 -k2,2 -t $'\t'
fi

if [ -f ${ngrams_counted} ] && [ ! -f ${ngrams_followerprecede_counted} ]; then
	echo "creating ${ngrams_followerprecede_counted}"
	time (export LC_ALL=C; cat ${ngrams_counted} | gzip -c -d | python ${py_base}/mr_ngram_nfollowerprecede.py -m | sort -k1,1 -t$'\t' --parallel=8 | python ${py_base}/mr_ngram_nfollowerprecede.py -r | gzip -c > ${ngrams_followerprecede_counted}) # -k1,1 -k2,2 -t $'\t'
fi

if [ -f ${ngrams_counted} ] && [ -f ${ngrams_follow_counted} ] && [ -f ${ngrams_precede_counted} ] && [ ! -f ${ngrams_joined_counts} ]; then
	echo "joining counts in ${ngrams_joined_counts}"
	time (export LC_ALL=C; join -a1 -e '' -1 1 -2 1 -t $'\t' <(cat ${ngrams_counted} | gzip -c -d) <(cat ${ngrams_precede_counted} | gzip -c -d) | join -a1 -e '' -1 1 -2 1 -t $'\t' - <(cat ${ngrams_follow_counted} | gzip -c -d) | join -a1 -e '' -1 1 -2 1 -t $'\t' - <(cat ${ngrams_followerprecede_counted} | gzip -c -d) | gzip -c > ${ngrams_joined_counts})
fi
