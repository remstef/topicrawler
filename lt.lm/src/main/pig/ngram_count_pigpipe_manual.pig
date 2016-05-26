/* ngram_count_pigpipe_manual.pig
*
* Count ngrams with hadoop using the python scripts and streaming api
* 
* Parameters:
* 	$in 		input directory
*
* Execution:
*
* 	pig -p in=<input-dir> -f ngram_count_pipe.pig
*
* 	pig -Dmapred.job.queue.name=<queue> -stop_on_failure -p in=<input-dir> -p min_count=<int> -f ngram_count_pipe.pig 
*
*/

%default min_count 1;
%declare BASEDIROUT '${in}_counts_m${min_count}';

SET default_parallel 1000;
SET mapred.map.tasks.speculative.execution false;
SET mapred.reduce.tasks.speculative.execution false;
SET debug off;
-- very_low, low, normal, high, very_high
SET job.priority high;

import './ngram_count_fun.pig' ;

count_ngrams( '${in}', '$BASEDIROUT/count', ${min_count} );
extract_vocabulary( '${in}', '$BASEDIROUT/vocab' );
count_nfollow( '$BASEDIROUT/count', '$BASEDIROUT/nfollow' );
count_nprecede( '$BASEDIROUT/count', '$BASEDIROUT/nprecede' );
count_nfollowerprecede( '$BASEDIROUT/count', '$BASEDIROUT/nfollowerprecede' );
join_ngram_counts( '$BASEDIROUT/count', '$BASEDIROUT/nfollow', '$BASEDIROUT/nprecede',  '$BASEDIROUT/nfollowerprecede',  '$BASEDIROUT/countsjoined' ); 






