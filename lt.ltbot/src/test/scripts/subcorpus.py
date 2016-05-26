#!/usr/bin/env python
# -*- coding: utf-8 -*-
##
#
# Requirements:
#
# - prepare sorted urls from crawl.log:
#
#  $ cat crawl.log | tr -s ' ' | cut -f1,4 -d' ' --output-delimiter=$'\t' | grep http > urls_sorted.txt
#
# - get one sentence per line with tab separated urls from webcorpus (SentenceExtractCompactJob)
#
# pipe sentence-file through this script, provide urls_sorted-file and number of sentences as parameters
#
#  $ zcat sentences.txt.gz | python3 subcorpus.py -f - -u urls_sorted.txt -o outputdir
#  
#  sort by time
#  $ cmd='cat stdin_0${n}.txt | sort -k1,1 --parallel 24 -T ~/tmp/ > stdin_0${n}_sorted.txt
#  $ for ((n=0;n<=9;++n)); do eval $cmd; done
#  merge
#  $ for ((n=0;n<=9;++n)); do cat stdin_0${n}_sorted.txt >> stdin_sorted.txt ; done
#
#  tokenize
#  $ for ((n=0;n<=9;++n)); do cat stdin_0${n}_sorted_2.txt | cut -f2 | seg.sh -s LineSplitter -l -fl 6 -nl 1 | cut -f2 > stdin_0${n}_sorted_2_sent_tok.txt & done
#  $ cat stdin_sorted.txt | cut -f2 | seg.sh -l -s LineSplitter -fl 5 -nl 2 | cut -f2 > stdin_sorted_sent_tok.txt
#  add token count
#  $ cat stdin_sorted_sent_tok.txt | ~/lib/heritrix-3.2.0/bin/num_tokens.py > stdin_sorted_sent_tok_num.txt
#
#  jlani language identification:
#  $ ~/lib/jlani$ java -cp lib/ASV_JLanI.jar de.uni_leipzig.asv.toolbox.jLanI.main.CLIMain <infile> <outfile>
#  $ grep "^de     " <jlani-outfile> | cut -f 5 > <jlani-infile-de>
#  get language distribution
#  $ grep -P -o "^.*? " <jlani-outfile> | sort | uniq -c > langdist.txt
#
# Other notes:
#  
#  create ngrams from splits
#  $ cmd='cat ${f} | cut -f1 |  lm.sh de.tudarmstadt.lt.lm.app.Ngrams | gzip -c > ${f}.ngrams.txt.gz'
#  $ for f in **/stdin_0*; do eval ${cmd} & done
#  $ cmd='cat ${m}/stdin_0${m}.txt.ngrams.txt.gz >> ${n}/${n}.ngrams.txt.gz'
#  $ for (( n=0 ; n<=9 ; ++n )) ; do for (( m=0 ; m<=n ; ++m )) ; do eval ${cmd} ; done; done
#
#  create ngram splits from all ngrams file, logarithmic: 100,300,1K,3K,10K,30K,100K,300K,1M,3M,10M,30M, ...
#  $ for i in {10K,30K,100K,300K,1M,3M,10M,30M,100M,300M,1000M,3000M}; do mkdir ${i}; done
#  $ cmd='zcat splits/all/all.ngrams.txt.gz | head -n ${i} | gzip -c > corpora/${i}/${i}.ngrams.txt.gz'
#  $ for i in {10K,30K,100K,300K,1M,3M,10M,30M,100M,300M,1000M,3000M}; do eval ${cmd} & done
#
#  create token splits from all ngrams file, logarithmic: 100,300,1K,3K,10K,30K,100K,300K,1M,3M,10M,30M, ...
#  $ for i in {10K,30K,100K,300K,1M,3M,10M,30M}; do mkdir ${i}; done
#  $ cmd='cat stdin_sorted_sent_tok_num.txt | ~/lib/heritrix-3.2.0/bin/head_tokens.py -n ${i} > corpora/${i}/stdin_sorted_sent_tok_num.txt'
#  $ for i in {100K,300K,1M,3M,10M,30M,100M,300M}; do eval ${cmd} & done
#
#  generate ngram index
#  $ for d in *; do lm.sh -Dlt.lm.sortparam="-T ${d} --parallel=4" -Dlt.lm.insertSentenceTags=1 de.tudarmstadt.lt.lm.app.GenerateNgramIndex -a -f ${d} -n 1-5 -m 1; done
#  $ for d in *; do lm.sh -Dlt.lm.sortparam="-T ${d} --parallel=4" -Dlt.lm.insertSentenceTags=3 -Dlt.lm.handleBoundaries=0 de.tudarmstadt.lt.lm.app.GenerateNgramIndex -f ${d} -n 1-5 -m 1 &  done
#
#  $ cmd='hdir="<basedir>/${n}"; ssh hn2 "hdfs dfs -mkdir ${hdir}"; cat ${n}/${n}.ngrams.txt.gz | ssh hn2 "gzip -c -d | hdfs dfs -put - ${hdir}/${n}.ngrams.txt"'
#  $ for ((n=0;n<=9;++n)); do eval ${cmd} & done
#
#  $ cmd='cd ~/lm/bin; pig -Dmapred.job.queue.name=shortrunning -stop_on_failure ngram_count_pigpipe_auto.py <basedir>/${n} 2'
#  $ for ((n=0;n<=9;++n)); do export n; screen -dmS cmd${n} bash -c "echo \"${cmd}\"; time ${cmd} ;exec bash" & done
#
#  $ for ((n=0;n<=9;++n)); do mkdir ${n}/.lmindex; touch ${n}/.lmindex/${n}.ngram.counts.txt.gz; touch $n/.lmindex/${n}.ngram.counts.nfollow.txt.gz; touch ${n}/.lmindex/${n}.ngram.counts.nprecede.txt.gz; touch ${n}/.lmindex/${n}.ngram.counts.nfollowerprecede.txt.gz; touch ${n}/.lmindex/${n}.ngram.counts.joined.txt.gz; touch ${n}/.lmindex/${n}.ngram.vocabulary.txt.gz ; done
#  $ cmd1='ssh hn2 "hdfs dfs -text <basedir>/${n}_counts_m2/countsjoined/p* | gzip -c " > ${n}/.lmindex/${n}.ngram.counts.joined.txt.gz'
#  $ cmd2='ssh hn2 "hdfs dfs -text <basedir>/${n}_counts_m2/vocab/p* | gzip -c " > ${n}/.lmindex/${n}.ngram.vocabulary.txt.gz'
#  $ for ((n=0;n<=9;++n)); do export n; bash -c "${cmd1}; ${cmd2}" & done
#
#  $ cmd='lm.sh de.tudarmstadt.lt.lm.app.StartLM -t ModifiedKneserNeyLM -pt LtSegProvider -n 5 -d ${n} -i <basename>_${n}'
#  $ for ((n=0;n<=9;++n)); do export n; screen -dmS lm${n} bash -c "echo \"${cmd}\"; ${cmd}" & done
#
#  within a tmux session:
#  start new window with rmi server
#  $ tmux new-window -n rmi -d "bin/lm.sh de.tudarmstadt.lt.lm.app.StartRMI; exec bash;"
#  create new window for the each lm in a pane
#  $ wname=<window-name>
#  $ tmux new-window -n ${wname} -d
#  command for running each command in a new window
#  $ cmd='id=$(basename ${d}); tmux new-window -n ${id} -d "bin/lm.sh de.tudarmstadt.lt.lm.app.StartLM -n 5 -m 2 -i ${id} -d ${d}"'
#  command for running each command in a new pane
#  $ cmd='id=$(basename ${d}); tmux split-window -t :${wname} -d "bin/lm.sh de.tudarmstadt.lt.lm.app.StartLM -n 5 -m 2 -i ${id} -d ${d}"; tmux select-layout -t :${wname} tiled'
#  $ for d in <lm-directories>; do echo ${d}; sleep 1; eval ${cmd}; done
#  $ for ((n=0;n<=9;++n)); do export d="<basedir>/${n}_m2_local"; eval ${cmd}; done
#  $ for n in {10K,30K,100K,300K,1M,3M,10M,30M,100M,300M,1000M,3000M}; do export d="<basedir>/${n}"; eval ${cmd} & done
#  
#  run perplexity client
#  $ cmd='lm.sh de.tudarmstadt.lt.lm.app.PerplexityClient -i ${id} -q -f data/lm/pedocs-test/pedocs.test.sentences.10.txt >> out.txt'
#  $ for ((n=0;n<=9;++n)); do export id="f_${n}_m2_local"; sleep 2; eval ${cmd}; done
#  $ for ((n=0;n<=9;++n)); do export id="nf_${n}_m2_local"; sleep 2; eval ${cmd}; done
#  $ for n in {10K,30K,100K,300K,1M,3M,10M,30M,100M,300M,1000M,3000M}; do export id="f2_${n}"; sleep 2; eval ${cmd} & done
#
#  $ ls data/lm/kdslbot-non-focused-edu-10/corpora_split_log_numtokens/* -d | grep -v 0M | while read d; do echo ${d} ; done
#  $ ls data/lm/kdslbot-non-focused-edu-10/corpora_split_log_numtokens | grep -v 0M | while read d; do echo ${d} ; done
#
#
##

from __future__ import print_function
import sys, os, gzip
import traceback
import click


def read_urls(f_urls):
    print('reading url file.', file=sys.stderr)
    urls = []
    s = 0
    for line in f_urls:
        s = s + 1
        line = line.rstrip()
        if not line:
            continue
        urls.append(tuple(line.split('\t')))
    print('read {} lines.'.format(s), file=sys.stderr);
    return urls
   
     
def split_linear(urls, num_splits=10):
    linear_split_urls = []
    s = len(urls)
    split_size = s / num_splits;
    for i in range(num_splits):
        b = int(i*split_size)
        if i == num_splits-1:
            e = s            
        else:
            e = int((i+1)*split_size)
        linear_split_urls.append(urls[b:e])
    return linear_split_urls


def split_sentences(url_splits, fin_sentences):
    print('reading sentences.', file=sys.stderr);
    i = 0;
    for line in fin_sentences:
        i = i + 1;
        line = line.rstrip()
        if not line:
            continue;
        try:
            s, n, date, urls = line.split('\t', 3)
            urls = [u.strip() for u in urls.split('\t')]
            for k, url_dict in enumerate(url_splits):
                for url in urls:
                    if url in url_dict:
                        yield k, s, url, url_dict[url]
                        break
                else:
                    continue  # executed if the loop ended normally (no break)
                break  # executed if 'continue' was skipped (break)
        except Exception as err:
            print('{}. (Line: {} - {})'.format(err, i, line), file=sys.stderr)
            traceback.print_exc()
    print('read {} lines.'.format(i), file=sys.stderr)

        
def write_urls(url_splits, outdir='.'):
    print('writing url splits to "{}".'.format(outdir), file=sys.stderr);
    for i, urls in enumerate(url_splits):
        with open(os.path.join(outdir, 'urls_{:02d}.txt'.format(i)), 'w') as fout:
            for url in urls:
                print('\t'.join(url), file=fout)
        url_splits[i] = dict([(url, time) for time, url in urls])
        
        
def write_sentences(sentence_triples, fprefix, outdir='.'):
    print('writing sentences to "{}".'.format(outdir), file=sys.stderr);
    files = {}
    for k, s, u, t in sentence_triples:
        if k in files:
            fout = files[k]
        else:
            fout = open(os.path.join(outdir, '{}_{:02d}.txt'.format(fprefix, k)), 'w')
            files[k] = fout
        print('{}\t{}\t{}'.format(t,s,u), file=fout)

    for fout in files.values():
        fout.flush()
        fout.close()


@click.command()
@click.option('-f', '--sentences',              help='File containing sentences and urls: one sentence per line, tab separated list of urls. Specify "-" to pipe this kind of content into this program.', type=click.File('r'), required=True)
@click.option('-u', '--urls',                   help='File containing urls: one per line.', type=click.File('r'), required=True)
@click.option('-s', '--numsplits', default=10,  help='Number of linear splits. (default=10)')
@click.option('-o', '--out', default='.',       help='Path to write splitted files into. (default=<current directory>)', type=click.Path(exists=True))
def run_cli(urls, numsplits, sentences, out):
    '''
      Generate supcorpora for evaluation.
    '''
    urls = read_urls(urls)
    url_splits = split_linear(urls, numsplits)
    write_urls(url_splits, outdir=out)
    sentence_triples = split_sentences(url_splits, sentences)
    write_sentences(sentence_triples, fprefix='stdin' if sentences.name == '<stdin>' else os.path.basename(sentences.name), outdir=out)

if __name__ == '__main__':
    run_cli()
