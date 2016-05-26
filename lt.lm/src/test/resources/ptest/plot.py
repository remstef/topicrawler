#!/usr/bin/env python3

from pandas import *
import os; print(os.getcwd())
import matplotlib.pyplot as plt
import sys

if len(sys.argv) > 1:
    f_eval = sys.argv[1]
else:
    f_eval = 'out.txt'

df = read_csv(f_eval, 
              sep='\t',
              header=None, 
              names=['lmtype', 'lmorder', 'testfile', 'perplexity'])

df = df[df['testfile'] == 'test' ]
df_r = df.pivot('lmorder', 'lmtype', 'perplexity')
print(df_r)

fig, axes = plt.subplots(nrows=1, ncols=2)
df_r[['BerkeleyLM','KneserNeyLM','PoptKneserNeyLM','PoptModifiedKneserNeyLM']].plot(ax=axes[0])
df_r[['BerkeleyLM_oov','KneserNeyLM_oov','PoptKneserNeyLM_oov','PoptModifiedKneserNeyLM_oov']].plot(ax=axes[1])
for ax in axes: ax.legend(loc='best')

plt.show()
plt.ylabel('Perplexity')