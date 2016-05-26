#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""


save for a particular ngram the amount of occurrneces where it serves in the middle at exactly once, twice and three times or more

test:
    cat data | map | sort | reduce
    cat data | ./x.py -m | sort | ./x.py -r

hadoop jar /opt/cloudera/parcels/CDH/lib/hadoop-mapreduce/hadoop-streaming.jar \
-files x.py \
-mapper 'x.py -m' \
-reducer 'x.py -r' \
-input in \
-output out

@author: stevo
"""

from __future__ import print_function
from __future__ import division
import itertools as it
import sys

def readlines():
    with sys.stdin as f:
        for line in f:
            if line.strip():
                yield line;

def mapper(lines):
    for line in lines:
        splits = line.rstrip().split('\t', 1)
        num = splits[1]
        ngram = splits[0].split(' ')
        if len(ngram) > 2:
            print('{}\t{}\t{}:x:{}'.format(' '.join(ngram[1:-1]), num, ngram[0], ngram[-1]))

def redline2tuple(lines):
    for line in lines:
        splits = line.rstrip().split('\t',2)
        yield tuple(splits)

def reducer(lines):
    for ngram, tuples in it.groupby(redline2tuple(lines), lambda t : t[0]):
        n=0; N1=0; N2=0; N3p=0
        for tuple_ in tuples:
            num = int(tuple_[1])
            n += num
            if num == 1:
                N1 += 1
            elif num == 2:
                N2 += 1
            else:
                N3p += 1
        print('{}\tn_fp:{},{},{},{}'.format(ngram,n,N1,N2,N3p))


if len(sys.argv) < 2:
    raise Exception('specify mapper (-m) or reducer (-r) function')

t = sys.argv[1]
if '-m' == t:
    mapper(readlines());
elif '-r' == t:
    reducer(readlines());
else:
    raise Exception('specify mapper (-m) or reducer (-r) function')