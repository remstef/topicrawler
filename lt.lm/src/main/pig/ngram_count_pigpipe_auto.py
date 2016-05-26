#!/usr/bin/python

###
#
# Count ngrams with hadoop using the pig functions, python scripts and streaming api
# 
# Parameters:
# 	- input directory
#
# Execution:
#
# 	pig ngram_count_pigpipe_auto.py <input-dir>
#
# 	pig -Dmapred.job.queue.name=<queue> -stop_on_failure ngram_count_pigpipe_auto.py <input-dir>
#
###

import sys, os
 
_header = """

	set default_parallel 1000;
	set job.name ${n};
	set mapred.map.tasks.speculative.execution false;
	set mapred.reduce.tasks.speculative.execution false;
	import './ngram_count_fun.pig' ;
	"""

if len(sys.argv) < 2:
	raise '\n No input path specified. \n'
_in = sys.argv[1].rstrip('/')

_min_count = 1
if len(sys.argv) > 2:
	_min_count = str(int(sys.argv[2]))

_out     = _in  + '_counts_m' + _min_count
_out_nc  = _out + '/count'
_out_v   = _out + '/vocab'
_out_nf  = _out + '/nfollow'
_out_np  = _out + '/nprecede'
_out_nfp = _out + '/nfollowerprecede'
_out_njc = _out + '/countsjoined'

##
# start actual pig jobs
#
from org.apache.pig.scripting import Pig 

# if output path does not exist, create it
if Pig.fs('-test -d ' + _out):
	Pig.fs('mkdir ' + _out)

##
# CountJob
#
# if output path of countjob already exists, skip it, run job
##
if not Pig.fs('-test -d ' + _out_nc):
	print '\nPath ("%s") already exists, skipping job.\n' % _out_nc
else:
	result = Pig.compile(_header + """
	count_ngrams( '${in}', '${out}', '${min_count}' );
	""").bind({'in':_in, 'out':_out_nc, 'min_count': _min_count, 'n':'count-ngrams'}).runSingle()
	# check the result
	if not result.isSuccessful():
	    raise "Pig job failed"


##
# ExtractVocabularyJob
#
# if output path of countjob already exists, skip it, run job
##
if not Pig.fs('-test -d ' + _out_v):
	print '\nPath ("%s") already exists, skipping job.\n' % _out_v
else:
	result = Pig.compile(_header + """
	extract_vocabulary( '${in}', '${out}' );
	""").bind({'in':_out_nc, 'out':_out_v, 'n':'extract-ngram-vocabulary'}).runSingle()
	# check the result
	if not result.isSuccessful():
	    raise "Pig job failed"


##
# NFollowCountJob
##
if not Pig.fs('-test -d ' + _out_nf):
	print '\nPath ("%s") already exists, skipping job.\n' % _out_nf
else:
	result = Pig.compile(_header + """
	count_nfollow( '${in}', '${out}' );
	""").bind({'in':_out_nc, 'out':_out_nf, 'n':'count-ngram-nfollow'}).runSingle()
	# check the result
	if not result.isSuccessful():
	    raise "Pig job failed"


##
# NPrecedeCountJob
##
if not Pig.fs('-test -d ' + _out_np):
	print '\nPath ("%s") already exists, skipping job.\n' % _out_np
else:
	result = Pig.compile(_header + """
	count_nprecede( '${in}', '${out}' );
	""").bind({'in':_out_nc, 'out':_out_np, 'n':'count-ngram-nprecede'}).runSingle()
	# check the result
	if not result.isSuccessful():
	    raise "Pig job failed"


##
# NFollowerPrecedeCountJob
##
if not Pig.fs('-test -d ' + _out_nfp):
	print '\nPath ("%s") already exists, skipping job.\n' % _out_nfp
else:
	result = Pig.compile(_header + """
	count_nfollowerprecede( '${in}', '${out}' );
	""").bind({'in':_out_nc, 'out':_out_nfp, 'n':'count-ngram-nfollowerprecede'}).runSingle()
	# check the result
	if not result.isSuccessful():
	    raise "Pig job failed"


##
# JoinCountsJob
##
if not Pig.fs('-test -d ' + _out_njc):
	print '\nPath ("%s") already exists, skipping job.\n' % _out_njc
else:
	result = Pig.compile(_header + """
	join_ngram_counts( '${in_nc}', '${in_nf}', '${in_np}',  '${in_nfp}',  '${out}' );
	""").bind({'in_nc':_out_nc, 'in_nf':_out_nf, 'in_np':_out_np, 'in_nfp':_out_nfp, 'out':_out_njc, 'n':'join-ngram-counts'}).runSingle()
	# check the result
	if not result.isSuccessful():
	    raise "Pig job failed"

print '\nScript finished sucessfully.\n'

