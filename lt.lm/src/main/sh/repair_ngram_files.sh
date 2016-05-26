#!/bin/bash
#set -e

# invoke with
#  find . -type d -name ".lmindex" -print -exec repair_ngram_files.sh {}/.. \;

get_abs_filename() {
  echo $(cd $1 && pwd)
}

#
basedir=${1}
basedir=$(get_abs_filename ${basedir})
name=$(basename ${basedir})
indexdir="${basedir}/.lmindex"

echo "   basedir:  ${basedir}"
echo "   name:     ${name}"
echo "   indexdir: ${indexdir}"

mv "${basedir}/${name}.ngrams.txt.gz" "${indexdir}/ngram.raw.txt.gz"
mv "${indexdir}/${name}.ngram.counts.txt.gz" "${indexdir}/ngram.counts.txt.gz"
mv "${indexdir}/${name}.ngram.counts.nfollow.txt.gz" "${indexdir}/ngram.counts.nfollow.txt.gz"
mv "${indexdir}/${name}.ngram.counts.nprecede.txt.gz" "${indexdir}/ngram.counts.nprecede.txt.gz"
mv "${indexdir}/${name}.ngram.counts.nfollowerprecede.txt.gz" "${indexdir}/ngram.counts.nfollowerprecede.txt.gz"
mv "${indexdir}/${name}.ngram.counts.joined.txt.gz" "${indexdir}/ngram.counts.joined.txt.gz"
mv "${indexdir}/${name}.ngram.vocabulary.txt.gz" "${indexdir}/ngram.vocabulary.txt.gz"
