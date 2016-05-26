#!/usr/bin/env python

from __future__ import print_function

import sys, os, lucene

from java.io import File
from org.apache.lucene.analysis.standard import StandardAnalyzer
from org.apache.lucene.index import DirectoryReader
from org.apache.lucene.index import Term
from org.apache.lucene.queryparser.classic import QueryParser
from org.apache.lucene.store import MMapDirectory
from org.apache.lucene.search import IndexSearcher
from org.apache.lucene.search import TermQuery
from org.apache.lucene.util import Version


class Lindex(object):
    
    def __init__(self, index_dir_ngram='./ngram', index_dir_vocab='./vocab' ):
        self._index_dir_ngram = index_dir_ngram;
        self._index_dir_vocab = index_dir_vocab;
        fs = MMapDirectory.open(File(index_dir_ngram))
        self._searcher_ngram = IndexSearcher(DirectoryReader.open(fs))
        
    def __getitem__(self, ngram): 
        pass;
        
    def search_interactive(self):
        doc = None
        while True:
            print()
            print('Hit enter with no input to quit.')
            command = raw_input('Query:')
            if command == '':
                return doc  
            print()
            print('Searching for:'.format(command))
            query = TermQuery(Term('ngram', command))
            hits = self._searcher_ngram.search(query, 1000).scoreDocs
            print('{} total matching documents.'.format(len(hits)))
            for i,hit in enumerate(hits):
                doc = self._searcher_ngram.doc(hit.doc)
                print('doc {}:'.format(i))
                for f in doc:
                    print(type(f))
                    print('\t{}'.format(f.toString().encode('utf-8')))                
#                print 'text:', doc.get("text")
                if i >= 30:
                    break;

    def search_interactive_history(self):
        doc = None
        while True:
            print()
            print('Hit enter with no input to quit.')
            command = raw_input('Query:')
            if command == '':
                return doc  
            print()
            print('Searching for:'.format(command))
            query = TermQuery(Term('history', command))
            hits = self._searcher_ngram.search(query, 1000).scoreDocs
            print('{} total matching documents.'.format(len(hits)))
            for i,hit in enumerate(hits):
                doc = self._searcher_ngram.doc(hit.doc)
                print('doc {}:'.format(i))
                for f in doc:
                    print(type(f))
                    print('\t{}'.format(f.toString().encode('utf-8')))                
#                print 'text:', doc.get("text")
                if i >= 30:
                    break;
            
       
if __name__ == '__main__':
    try:
        lucene.findClass('org/apache/lucene/document/Document')
    except:
        lucene.initVM(vmargs=['-Djava.awt.headless=true'])
    print('lucene {}'.format(lucene.VERSION))