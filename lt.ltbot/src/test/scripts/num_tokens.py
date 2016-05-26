#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu Jan 22 19:47:49 2015

@author: stevo
"""

import sys
import click
from signal import signal, SIGPIPE, SIG_DFL; signal(SIGPIPE,SIG_DFL) 

def sentence_tok_nr(fin):
    print('reading sentences file.', file=sys.stderr)
    s = 0
    num_all_tokens = 0
    for line in fin:
        s = s + 1
        if s % 10000 == 0:
            print('read {} lines.'.format(s), file=sys.stderr);
        line = line.rstrip()
        if not line:
            continue
        num_tokens = len(line.split(' '))
        num_all_tokens += num_tokens
        print('{}\t{}\t{}\t{}'.format(line,num_all_tokens,num_tokens,s), file=sys.stdout)
    print('read {} lines.'.format(s), file=sys.stderr);

@click.command()
@click.option('-f', '--sentences', help='File containing one sentence per line. Specify "-" to read from stdin.', type=click.File('r'), required=True, default='-')
def run_cli(sentences):
    '''
      add the number of tokens for the current line and sum of tokens after the current line from the beginning
    '''
    sentence_tok_nr(sentences)

if __name__ == '__main__':
    run_cli()
