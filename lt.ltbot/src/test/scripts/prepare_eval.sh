#!bin/bash

b=$HOME/data/texeval-2015

# create the directories and documents
for t in ai-en vehicles-en plants-en; do for m in nf 1 2 3 5; do d=$t-$m; for s in 100K 300K 1M 3M 10M 30M 100M; do mkdir -p $b/$d/$s; zcat $b/$d/crawl-sentences.txt.gz | cut -f1 | python3 ~/git/lt.kd/lt.ltbot/src/test/scripts/num_tokens.py | python3 ~/git/lt.kd/lt.ltbot/src/test/scripts/head_tokens.py -n $s > $b/$d/$s/crawl-sentences.txt & done; done; done

# copy the original training data
for t in ai-en vehicles-en plants-en; do for m in nf 1 2 3 5; do d=$t-$m; tg=$b/$t/lm-small; for s in 0 100K 300K 1M 3M 10M 30M 100M; do mkdir -p $b/$d/$s; ln -s $tg/*.txt $b/$d/$s/ ; done; done; done

# create test data
cat articles-sub-artificial_intelligence-docs.tsv | cut -f3 | sed 's/\\n/\n/g' | seg -fl 6 | cut -f2 | lm-nightly -Dlt.lm.insertSentenceTags=0 -Dlt.lm.handleBoundaries=-1 de.tudarmstadt.lt.lm.app.Ngrams -f - -o - -a false -p PreTokenizedStringProvider -n 3 > test-ngrams.txt

# start rmi
lm-nightly de.tudarmstadt.lt.lm.app.StartRMI 

# start training lm 
j=ai-en; d=$HOME/data/texeval-2015/$j/lm-small; lm-nightly -Xmx10g -Dlt.lm.knUnkLog10Prob=-10 de.tudarmstadt.lt.lm.app.StartLM -t BerkeleyLM -n 5 -d $d -i $j-train

# start lm 
j=ai-en-5; s=100M; lm-nightly -Xmx10g -Dlogfile=$HOME/logs/$j-$s-lm.log -Dlt.lm.knUnkLog10Prob=-10 de.tudarmstadt.lt.lm.app.StartLM -t BerkeleyLM -n 3 -d $b/$j/$s -i $j-$s

# calculate perplexity (--one_ngram_per_line -p 10990)
j=ai-en; m=$j-1-100M; r=$b/eval/eval-$j.tsv; lm-nightly -Dlogfile=$HOME/logs/$j-$s-pc.log -Dverbosity=INFO de.tudarmstadt.lt.lm.app.PerplexityClient --one_ngram_per_line --noov true -i $m -q -f $b/$j/test-ngrams.txt --oovreflm $j-train >> $r
# test
j=ai-en; m=$j-1-100M; r=$b/eval/eval-$j.tsv; cat $b/$j/test-ngrams.txt | head -n 10000 | lm-nightly -Dverbosity=INFO de.tudarmstadt.lt.lm.app.PerplexityClient --one_ngram_per_line --noov true -i $m -q --oovreflm $j-train 

# calculate document perplexities
for t in $topics; do for n in $models; do d=$t-$n; { ls -dtr $d/job/2016* | while read dd; do { ls -dtr $dd/sentences/HTML* | while read f; do echo $f; done; } done | while read f; do zcat $f; done; } | lm-nightly -Dlogfile=$HOME/logs/$j-$s-pd.log -Dverbosity=WARN de.tudarmstadt.lt.lm.app.PerpDoc -i $t-train-3 -q > $d/perpdocs-reflm.txt; done; done 


topics="ai-en vehicles-en plants-en"
models="nf 1 2 3 5"
sizes="100K 300K 1M 3M 10M 30M 100M"

for t in $topics; do for m in $models; do for s in $sizes; do echo $t $m $s & done; wait; done; done

	
for t in ai-en vehicles-en plants-en; do for m in nf 5; do d=$b/$t-$m; zcat $d/crawl-sentences.txt.gz | cut -f1 | sort | uniq > $d/crawl-sentences-u.txt & done; done
for t in ai-en vehicles-en plants-en; do for m in nf 5; do d=$b/$t-$m; zcat $d/crawl-sentences-pr1e4.txt.gz | cut -f1 | sort | uniq > $d/crawl-sentences-pr1e4-u.txt & done; done

# jlani	
jld=$HOME/local/jlani; cwd=$(pwd); cd $jld; for t in ai-en vehicles-en plants-en; do for m in nf 5; do d=$b/$t-$m; java -cp .:./lib/ASV_JLanI.jar -Djava.ext.dirs=.:./lib de.uni_leipzig.asv.toolbox.jLanI.main.CLIMain $d/crawl-sentences-u.txt $d/crawl-sentences-u-lang.tsv & done; done; cd $cwd
for t in ai-en vehicles-en plants-en; do for m in nf 5; do d=$b/$t-$m; cat $d/crawl-sentences-u-lang.tsv | cut -f1,5 | grep $'^en\t' | cut -f2 | gzip -c > $d/crawl-sentences-u-en.txt.gz & done; done
	
jld=$HOME/local/jlani; cwd=$(pwd); cd $jld; for t in ai-en vehicles-en plants-en; do for m in nf 5; do d=$b/$t-$m; java -cp .:./lib/ASV_JLanI.jar -Djava.ext.dirs=.:./lib de.uni_leipzig.asv.toolbox.jLanI.main.CLIMain $d/crawl-sentences-pr1e4-u.txt $d/crawl-sentences-pr1e4-u-lang.tsv & done; done; cd $cwd
for t in ai-en vehicles-en plants-en; do for m in nf 5; do d=$b/$t-$m; cat $d/crawl-sentences-pr1e4-u-lang.tsv | cut -f1,5 | grep $'^en\t' | cut -f2 | gzip -c > $d/crawl-sentences-pr1e4-u-en.txt.gz & done; done
	
# extract perplexity values and via perplexity values from crawl log
cat crawl.log* | grep perp\" | perl -lanE '$_ =~ /perp\":\"(.*?)\",/; $perp=$1; print $F[0], "\t", $F[3], "\t", $perp, "\t", "-", "\t", $F[5]'
		# ned:~/local/lt.ltbot-0.4.0c-SNAPSHOT/heritrix-3.2.0/jobs/vehicles-en/latest/logs$ cat crawl.log* | grep perp\" | perl -lanE '$_ =~ /perp\":\"(.*?)\",/; $perp=$1; print $F[0], "\t", $F[3], "\t", $perp, "\t", "-", "\t", $F[5]' > $b/vehicles-en-nf/perp-crawl.log &
#sample line:  ^2016-07-21T10:34:43.615Z   200      70767 http://www.kbb.com/chevrolet/convertibles/ - - text/html #024 20160721103441795+670 - - - {"perp":"000000225426","warcFilename":"HTML-20160721102255397-00000-19677~ned.ukp.informatik.tu-darmstadt.de~8443.warc.gz","		
cat crawl.log* | grep perp\" | grep perp_ | perl -lanE '$_ =~ /perp\":\"(.*?)\",/; $perp=$1; $_ =~ /perp\_via\":\"(.*?)\",/; $perpvia=$1; print $F[0], "\t", $F[3], "\t", $perp, "\t", $perpvia, "\t", $F[5]' > perp.tsv
find . -name "latest" | sort | while read d; do a=$(dirname $d); td=$b/${a:2}; ls -tr $d/logs/crawl.log* | xargs cat - | grep perp\" | grep perp_ | perl -lanE '$_ =~ /perp\":\"(.*?)\",/; $perp=$1; $_ =~ /perp\_via\":\"(.*?)\",/; $perpvia=$1; print $F[0], "\t", $F[3], "\t", $perp, "\t", $perpvia, "\t", $F[5]' > $td/perp-crawl.log; done

for t in ai-en vehicles-en plants-en; do for m in nf 1 2 3 5; do d=$b/$t-$m; cat -n $d/job/latest/logs/crawl.log*  | perl -lanE 'if( $F[5] =~ /\-/ ) { print $_  }' | grep perp\" | perl -lanE '$_ =~ /perp\":\"(.*?)\",/; $perp=$1; print $F[0],"\t",$F[1],"\t",$perp,"\t",$F[4],"\t-\t",$F[6]' > $d/perp-crawl-seed.log; done; done


find . -name docperps.tsv | while read f; do d=$(dirname $f); cat $f | perl -F\\t -lanE 'if($F[1] =~ /.*[0-9].*/ && $F[1]<1e4){print $_}' > $d/docperps-pr1e4.tsv &  done


dirs="ai-en-5 plants-en-5 vehicles-en-5"; for d in $dirs; do zcat $d/crawl-sentences.txt.gz | cut -f1 | head -c 11GB > $d/crawl-sentences-11GB.txt; done

### steffen@farnsworth:/mnt/farnsworthshare/semeval-2015-task17-texeval/wiki$ zcat wikipedia.txt.gz | wc
### 109136195 1868200724 11792896391
### wikipedia 11.8GB non-unique

dirs="ai-en-nf vehicles-en-nf plants-en-nf"
for d in $dirs; do jobs=$(ls -tr $d | grep job); for j in $jobs; do files=$(ls -tr $d/$j/sentences/ | grep "HTML.*\.txt\.gz"); for f in $files; do zcat $d/$j/sentences/$f | cut -f2,5 | gzip -c >> $d/crawl-sentences.txt.gz ; done ; done & done

# check size
fun () { zcat $1 | wc > $1.wc; }; for d in $dirs; do fun $d/crawl-sentences.txt.gz & done
	
dirs="ai-en-nf plants-en-nf vehicles-en-nf"; for d in $dirs; do zcat $d/crawl-sentences.txt.gz | cut -f1 | head -c 11GB > $d/crawl-sentences-11GB.txt; done

# combine function with find command
fun () { echo $1; ls -lah $1; }; find . -maxdepth 1 -type d -name "*-nf" | while read d; do fun $d; done

# sync to farnsworth 	
fun () { rsync -avvzhP $1/crawl-sentences* fw:data/semeval-2015-task17-texeval/$1/ ;  }
find . -maxdepth 1 -type d -name "*-nf" | while read d; do fun $d & done

	 

	