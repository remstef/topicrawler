/* ngram_count_fun.pig
*
* Count ngrams with hadoop using the python scripts and streaming api
* 
*/
/*
DEFINE foreach_count(A, C) RETURNS B {
   $B = FOREACH $A GENERATE group, COUNT($C);
};
*/

DEFINE load_counted_ngrams( in_count ) 
RETURNS ngrams_counted {
	$ngrams_counted = LOAD '${in_count}' USING PigStorage('\t', '-noschema') AS (ngram:bytearray, n:long);
};


DEFINE count_ngrams( in_ngram, out_count, min ) 
RETURNS void {

	NGRAMS 					= LOAD '${in_ngram}' USING PigStorage('\n', '-noschema') AS (ngram: bytearray);
	NGRAMS_GROUPED 			= GROUP NGRAMS BY *; 
	NGRAMS_COUNTED 			= FOREACH NGRAMS_GROUPED GENERATE group, (long)COUNT(NGRAMS);
	NGRAMS_COUNTED_F 		= FILTER NGRAMS_COUNTED BY $1 >= $min;
	STORE NGRAMS_COUNTED_F INTO '${out_count}' USING PigStorage('\t');

};


DEFINE count_ngrams_stream( in_ngram, out_count ) 
RETURNS void {

	DEFINE ngram_count_map `python mr_ngram_count.py -m` ship('mr_ngram_count.py') input(stdin using PigStreaming('\t')) output(stdout using PigStreaming('\t'));
	DEFINE ngram_count_red `python mr_ngram_count.py -r` ship('mr_ngram_count.py') input(stdin using PigStreaming('\t')) output(stdout using PigStreaming('\t'));
	NGRAMS 					= LOAD '${in_ngram}' USING PigStorage('\t', '-noschema') AS (ngram:bytearray);
	NGRAMS_COUNTED 			= STREAM NGRAMS THROUGH ngram_count_map;
	NGRAMS_COUNTED_grp 		= GROUP NGRAMS_COUNTED BY $0;
	NGRAMS_COUNTED_flt		= FOREACH NGRAMS_COUNTED_grp GENERATE FLATTEN(NGRAMS_COUNTED);
	NGRAMS_COUNTED_fin 		= STREAM NGRAMS_COUNTED_flt THROUGH ngram_count_red;
	STORE NGRAMS_COUNTED_fin INTO '${out_count}' USING PigStorage('\t');	

};


DEFINE extract_vocabulary( in_count, out_vocab ) 
RETURNS void {

	NGRAMS_COUNTED 	= LOAD '${in_count}' USING PigStorage('\t', '-noschema') AS (ngram:bytearray);
	VOCAB 			= FOREACH NGRAMS_COUNTED GENERATE FLATTEN(TOKENIZE(ngram)) as word;
	VOCAB_grp 		= GROUP VOCAB BY word;
	VOCAB_u 		= FOREACH VOCAB_grp GENERATE group;
	STORE VOCAB_u INTO '${out_vocab}' USING PigStorage('\t');

};


DEFINE count_nfollow( in_count, out_nfollow ) 
RETURNS void {

	DEFINE ngram_nfollow_map `python mr_ngram_nfollow.py -m` ship ('mr_ngram_nfollow.py') input(stdin using PigStreaming('\t')) output(stdout using PigStreaming('\t'));
	DEFINE ngram_nfollow_red `python mr_ngram_nfollow.py -r` ship ('mr_ngram_nfollow.py') input(stdin using PigStreaming('\t')) output(stdout using PigStreaming('\t'));
	NGRAMS_COUNTED			= LOAD '${in_count}' USING PigStorage('\t', '-noschema') AS (ngram:bytearray, n:long);
	NGRAMS_NFOLLOW 			= STREAM NGRAMS_COUNTED THROUGH ngram_nfollow_map;
	NGRAMS_NFOLLOW_grp 		= GROUP NGRAMS_NFOLLOW BY $0;
	NGRAMS_NFOLLOW_flt		= FOREACH NGRAMS_NFOLLOW_grp GENERATE FLATTEN(NGRAMS_NFOLLOW);
	NGRAMS_NFOLLOW_fin 		= STREAM NGRAMS_NFOLLOW_flt THROUGH ngram_nfollow_red;
	STORE NGRAMS_NFOLLOW_fin INTO '${out_nfollow}' USING PigStorage();

};


DEFINE count_nprecede( in_count, out_nprecede ) 
RETURNS void {

	DEFINE ngram_nprecede_map `python mr_ngram_nprecede.py -m` ship ('mr_ngram_nprecede.py') input(stdin using PigStreaming('\t')) output(stdout using PigStreaming('\t'));
	DEFINE ngram_nprecede_red `python mr_ngram_nprecede.py -r` ship ('mr_ngram_nprecede.py') input(stdin using PigStreaming('\t')) output(stdout using PigStreaming('\t'));
	NGRAMS_COUNTED			= LOAD '${in_count}' USING PigStorage('\t', '-noschema') AS (ngram:bytearray, n:long);
	NGRAMS_NPRECEDE			= STREAM NGRAMS_COUNTED THROUGH ngram_nprecede_map;
	NGRAMS_NPRECEDE_grp 	= GROUP NGRAMS_NPRECEDE BY $0;
	NGRAMS_NPRECEDE_flt		= FOREACH NGRAMS_NPRECEDE_grp GENERATE FLATTEN(NGRAMS_NPRECEDE);
	NGRAMS_NPRECEDE_fin 	= STREAM NGRAMS_NPRECEDE_flt THROUGH ngram_nprecede_red;
	STORE NGRAMS_NPRECEDE_fin INTO '${out_nprecede}' USING PigStorage();
};


DEFINE count_nfollowerprecede( in_count, out_nfollowerprecede ) 
RETURNS void {

	DEFINE ngram_nfollowerprecede_map `python mr_ngram_nfollowerprecede.py -m` ship ('mr_ngram_nfollowerprecede.py') input(stdin using PigStreaming('\t')) output(stdout using PigStreaming('\t'));
	DEFINE ngram_nfollowerprecede_red `python mr_ngram_nfollowerprecede.py -r` ship ('mr_ngram_nfollowerprecede.py') input(stdin using PigStreaming('\t')) output(stdout using PigStreaming('\t'));
	NGRAMS_COUNTED					= LOAD '${in_count}' USING PigStorage('\t', '-noschema') AS (ngram:bytearray, n:long);
	NGRAMS_NFOLLOWERPRECEDE			= STREAM NGRAMS_COUNTED THROUGH ngram_nfollowerprecede_map;
	NGRAMS_NFOLLOWERPRECEDE_grp 	= GROUP NGRAMS_NFOLLOWERPRECEDE BY $0;
	NGRAMS_NFOLLOWERPRECEDE_flt		= FOREACH NGRAMS_NFOLLOWERPRECEDE_grp GENERATE FLATTEN(NGRAMS_NFOLLOWERPRECEDE);
	NGRAMS_NFOLLOWERPRECEDE_fin 	= STREAM NGRAMS_NFOLLOWERPRECEDE_flt THROUGH ngram_nfollowerprecede_red;
	STORE NGRAMS_NFOLLOWERPRECEDE_fin INTO '${out_nfollowerprecede}' USING PigStorage();
	
};


DEFINE join_ngram_counts( in_count, in_nfollow, in_nprecede, in_nfollowerprecede, out_joined ) 
RETURNS void {

	NGRAMS_COUNTED			= LOAD '${in_count}' USING PigStorage('\t') AS (ngram:bytearray, n:long);
	NGRAMS_NFOLLOW			= LOAD '${in_nfollow}' USING PigStorage('\t') AS (ngram:bytearray, nf:bytearray);
	NGRAMS_NPRECEDE			= LOAD '${in_nprecede}' USING PigStorage('\t') AS (ngram:bytearray, np:bytearray);
	NGRAMS_NFOLLOWERPRECEDE = LOAD '${in_nfollowerprecede}' USING PigStorage('\t') AS (ngram:bytearray, nfp:bytearray);
	NGRAMS_JOINED = JOIN NGRAMS_COUNTED by ngram LEFT, NGRAMS_NFOLLOW by ngram;
	NGRAMS_JOINED = JOIN NGRAMS_JOINED by $0 LEFT, NGRAMS_NPRECEDE by ngram;
	NGRAMS_JOINED = JOIN NGRAMS_JOINED by $0 LEFT, NGRAMS_NFOLLOWERPRECEDE by ngram;
	NGRAMS_JOINED = FOREACH NGRAMS_JOINED GENERATE NGRAMS_COUNTED::ngram, NGRAMS_COUNTED::n, NGRAMS_NFOLLOW::nf, NGRAMS_NPRECEDE::np, NGRAMS_NFOLLOWERPRECEDE::nfp;
	STORE NGRAMS_JOINED INTO '${out_joined}' USING PigStorage('\t');
	
};

