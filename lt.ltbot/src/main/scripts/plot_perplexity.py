#!/usr/bin/python3
# -*- coding: utf-8 -*-

import numpy as np;
import matplotlib.pyplot as plt;
# from scipy import interpolate;
import sys;

def plot_perplexity(fn, binsize=100, xlimits=(0,200000), polydeg=0, avgfun=np.median, ylimits=None, xtickstep=50000, avg_line_binsize=1000, limit_avg=False, plot_dots=True, show_total_average=True, **kwargs):
    tuples = read_perlpexity_log(fn,limit=xlimits[1])
    plot_time_vs_perp(tuples, binsize, xlimits, polydeg, avgfun, ylimits, xtickstep, avg_line_binsize, limit_avg, plot_dots, show_total_average,  **kwargs);

def plot_perplexity_histogram(fn, bins=100, range=(0,100000), **kwargs):
    """
    arguments are copied from matplotlib.pyplot.hist::

      def plot_perplexity_histogram(perplexity_log_filename, bins=100, range=(0,100000), normed=False, weights=None,
             cumulative=False, bottom=None, histtype='bar', align='mid',
             orientation='vertical', rwidth=None, log=False,
             color=None, label=None,
             **kwargs):
    """
    tuples = read_perlpexity_log(fn);
    plot_perp_hist(tuples, bins=bins, range=range, **kwargs);

def read_perlpexity_log(fn, limit=float('inf')):
    print('reading file {}.'.format(fn));
    with open(fn) as fh:
        i = 0;
        for line in fh:
            i = i+1;
            if i % 1e4 == 0:
                print('.', file=sys.stderr, end='');
                if i % 1e6 == 0 :
                    print('', file=sys.stderr);
            line = line.strip();
            (ts, perp, text, url) = line.split('\t',3);
            yield ts, perp, text, url;
            if i >= limit:
                break;
    print('\nread {} lines.'.format(i), file=sys.stderr);

def plot_perp_hist(tuples, count=-1, **kwargs):
    perp_values = np.fromiter(map(lambda t: t[1], tuples), dtype=np.float, count=count);
    print(kwargs)
    n, bins, patches = plt.hist(perp_values, **kwargs);
    return n, bins, patches

def plot_time_vs_perp(tuples, binsize=100, xlimits=(0,200000), polydeg=0, avgfun=np.mean, ylimits=None, xtickstep=50000, avg_line_binsize=10000, limit_avg=False, plot_dots=True, show_total_average=True, **kwargs):
    print('plotting perplexity values as a function over time.');
    yl = np.array([(perp, ts) for (ts, perp, text, url) in tuples if int(ts) > 0 and float(perp) > 1], dtype=np.float);
    y = yl[:,0];
    if ylimits and limit_avg:
        print('bounding')
        y[y >= ylimits[1]] = ylimits[1]
        y[y <= ylimits[0]] = ylimits[0]
    if binsize > 1:
        y = np.fromiter(map(avgfun, sublists(y, binsize)), dtype=np.float, count=-1);
    x = np.arange(len(y));
    beg = max(0,xlimits[0]);
    end = min(len(y), xlimits[1]);
    y = y[beg:end];
    print('total number of datapoints collected: {}; binsize: {}; number of new (averaged) datapoints: {}; limits: {}; number of averaged datapoints showing: {};'.format(len(yl), binsize, len(x), xlimits, len(y)));
    x = x[beg:end];
    if plot_dots:
        plt.plot(x, y, 'b.', ms=2, mec='b',mew=1,**kwargs);
    if show_total_average:
        avg_total = avgfun(y);
        y_avg_total = np.empty(len(x));
        y_avg_total.fill(avg_total)
        plt.plot(x, y_avg_total, 'y-', linewidth=2);
        print('total average according to avgfun: {}'.format(avg_total));
    y_avg = np.zeros(len(y));
    for i in range(len(y)):
        max_i = min(i+avg_line_binsize,len(y));
        min_i = max(i-avg_line_binsize,0);
        window_y = y[min_i:max_i];
        y_avg[i] = avgfun(window_y);
    #print(y);
    #print(y_avg);
    plt.plot(x, y_avg, 'r-', linewidth=2);


    if polydeg > 0:
        print('fitting data to a polynomial with degree {}.'.format(polydeg));
        # least squares polynomial fit to perplexity in log scale
        p_coeff = np.polyfit(x, np.log(y), polydeg);
        print('calculating values for polynomial...');
        p = np.poly1d(p_coeff);
        ynew = np.exp(p(x));
        #p = interpolate.interp1d(x, log_y);
        #p = interpolate.interp1d(x, log_y, kind='cubic');
        # spline interpolation
        #tck = interpolate.splrep(x,log_y,s=0)
        #ynew = interpolate.splev(x,tck,der=0)
        print('... finished calculating.');
        plt.plot(x, ynew, 'm-', linewidth=2);
    xlimits = [int(i / binsize) for i in xlimits];
    xtickstep = int(xtickstep / binsize);
    plt.xlim(xlimits);
    xticks = range(xlimits[0], xlimits[1], xtickstep);
    xticklabels = [binsize*tick for tick in xticks];
    plt.xticks(xticks, xticklabels);
    if ylimits:
        plt.ylim(ylimits);
    #plt.gca().set_yscale('log');
    #plt.show();

def sublists(values, binsize=100):
    indexes = list(range(0,len(values),binsize));
    indexes.append(len(values));
    for i in range(len(indexes)-1):
        j = i+1;
        sublist = values[indexes[i]:indexes[j]];
        yield sublist;





