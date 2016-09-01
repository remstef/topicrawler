#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu Jan 22 19:47:49 2015

@author: stevo
"""

import sys
import click
from signal import signal, SIGPIPE, SIG_DFL; signal(SIGPIPE,SIG_DFL) 


def head(fin, numtokens):
    print('reading sentences file.', file=sys.stderr)
    num_tokens_printed = 0;
    c1 = 0
    c2 = 0;
    for line in fin:
        c1 += 1
        if num_tokens_printed > numtokens:
            break
        line = line.rstrip()
        if not line:
            continue
        splits = line.split('\t')
        print(splits[0], file=sys.stdout)
        num_tokens_printed = int(splits[1])
        c2 += 1
    print('read {} lines. Printed {} tokens from {} sentences.'.format(c1, num_tokens_printed, c2), file=sys.stderr);

@click.command()
@click.option('-f', '--sentences', help='File containing one sentence per line incl. counts. Specify "-" to read from stdin.', type=click.File('r'), required=True, default='-')
@click.option('-n', '--numtokens', help='Print the first sentences that have maximally n number of tokens in sum.', default='10')
def run_cli(sentences, numtokens):
    '''
      add the number of tokens for the current line and sum of tokens after the current line from the beginning
    '''
    numtokens=int(numtokens.replace('G','KM').replace('M','KK').replace('K','000'))
    head(sentences, numtokens)

if __name__ == '__main__':
    run_cli()
