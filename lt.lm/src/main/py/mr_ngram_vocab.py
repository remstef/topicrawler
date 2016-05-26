#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""

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
        tokens = line.strip().split('\t',1)[0].split(' ')
        for token in tokens:
            print(token)

def reducer(lines):
    for key, values in it.groupby(lines, lambda t : t.rstrip()):
        print(key)

if len(sys.argv) < 2:
    raise Exception('specify mapper (-m) or reducer (-r) function')

t = sys.argv[1]
if '-m' == t:
    mapper(readlines());
elif '-r' == t:
    reducer(readlines());
else:
    raise Exception('specify mapper (-m) or reducer (-r) function')