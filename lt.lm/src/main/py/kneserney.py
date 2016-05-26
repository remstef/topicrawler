import logging; logging.basicConfig(format='%(asctime)s %(levelname)s %(name)s: %(message)s', level=logging.ERROR)
import nltk;
from nltk import FreqDist
from nltk.util import ngrams

_logger = logging.getLogger('kneserney')
_logger.setLevel(logging.INFO)  # 8, logging.DEBUG, logging.INFO


_max_order = 5
_ngrams    = [None]*(_max_order+1)
_logger.debug('maxorder = {}'.format(_max_order))

def numfollow(ngram):
    '''
    Calculate N_1+(ngram,x)
    '''
    # num words that occur after the ngram
    N1p = 0
    higher = _ngrams[len(ngram)+1]
    for ngram_ in higher:
        if ngram_[:-1] == ngram:
            N1p += 1
            
    return N1p

def numprecede(ngram):
    '''
    Calculate N_1+(x,ngram)
    '''
    # num words that occur before the ngram
    N1p = 0
    higher = _ngrams[len(ngram)+1]
    for ngram_ in higher:
        if ngram_[1:] == ngram:
            N1p += 1
    return N1p

def numfollowerprecede(ngram):
    '''
    Calculate N1+(x,ngram,x) = \sum_wi N_1+(x,ngram,wi)
    '''
    # num ngrams where the current ngram occurs in the middle, i.e. one word before and one after
    N1p = 0
    two_higher = _ngrams[len(ngram)+2]
    for ng in two_higher:
        if ng[1:-1] == ngram:
            N1p += 1
    return N1p
    # eqivalently: for each follower that includes the current ngram compute the number of preceding ngrams
    # N1p = 0
    # higher = _ngrams[len(ngram)+1]
    # for ngram_ in higher:
    #     if ngram_[:-1] == ngram:
    #         N1p += numprecede(ngram_)
    # return N1p

def num(ngram):
    return _ngrams[len(ngram)][ngram]

def numbigrams():
    return len(_ngrams[2])

def numunigrams():
    return len(_ngrams[1])

def kn(ngram, d=0.7, islower=False, num_recursions=10):
    ngram = tuple(ngram)
    n = len(ngram)
    hist  = ngram[:-1]
    lower = ngram[1:]
    
    if (not islower) and n > 1:
        # highest order prob and weight 
        c      = num(ngram)
        c_hist = num(hist)
        _logger.log(9,('{:<50} :count={count:d} / count_hist={count_hist:d}').format(('{!s:<10}'*n).format(*ngram), count=c, count_hist=c_hist))
        if num_recursions == 0: # recursion stop?
            p = 0 if c_hist == 0 else c / c_hist
            pkn = p; lw = 0; lp = 0
            _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
            return pkn

        
        if c_hist == 0:
            lp = kn(lower, d, False, num_recursions-1)
            p = 0; lw = d; pkn = lw * lp 
            _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
            return pkn
        lp = kn(lower, d, True, num_recursions-1)
        p  = max(c - d, 0) / c_hist
        lw = (d / c_hist) * numfollow(hist)
        pkn = p + lw * lp
        _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
        return pkn
    elif n > 1:
        # continuation prob and weight for bigrams++
        denom = numfollowerprecede(hist)
        nom = numprecede(ngram)
        _logger.log(9,('{:<50} :num_precede={num_precede:d} / num_followerprecede={num_followerprecede:d}').format(('{!s:<10}'*n).format(*ngram), num_precede=nom, num_followerprecede=denom))
        if num_recursions == 0: # recursion stop?
            p = 0 if denom == 0 else nom / denom
            pkn = p; lw = 0; lp = 0
            _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
            return pkn
        lp = kn(lower, d, True, num_recursions-1)
        if denom == 0:
            raise Exception('whaaaaatt??')
            # p = 0; lw = d; pkn = lw * lp
            # _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
            # return pkn
        p  = max(nom-d,0) / denom
        lw = (d / denom) * numfollow(hist)
        pkn = p + lw * lp
        _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
        return pkn
    # continuation prob and weight for unigrams
    denom = numbigrams() # divide by number of bigrams    
    nom = numprecede(ngram)
    _logger.log(9,('{:<50} :num_precede={num_precede:d} / num_bigrams={num_bigrams:d}').format(('{!s:<10}'*n).format(*ngram), num_precede=nom, num_bigrams=denom))
    if num_recursions == 0: # recursion stop?
        p = 0 if denom == 0 else nom / denom
        pkn = p; lw = 0; lp = 0
        _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
        return pkn
    p  = max(nom-d,0) / denom
    lw = (d / denom)
    lp = 1 / (numunigrams()+1) # uniform prob
    pkn = p + lw * lp
    _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
    return pkn

def predict_next(ngram_history, num_top_words=1000, num_skip_words=0, **kwargs):
    ngram_history = tuple(ngram_history[-_max_order+1:])
    max_p    = -1
    max_word = None
    # for word in _ngrams[1]:
    for word,_ in _ngrams[1].most_common(num_top_words+num_skip_words)[num_skip_words:]:
        ngram=ngram_history+word
        p = kn(ngram, num_recursions=1, **kwargs)
        if p > max_p:
            max_p = p
            max_word = word
    return max_word, max_p

def predict_sentence(max_length=5, **kwargs):
    _logger.log(15,'Starting a new prediction sequence.')
    sequence = ('<s>',)
    w = None
    i = 0
    while w != ('</s>',) and i < max_length:
        w,p = predict_next(sequence, **kwargs);
        sequence += w
        i += 1
        _logger.log(15,"Added word '{}' with prob {} to sequence.".format(w[0], p))
    print(sequence)

# main
for i in range(1,len(_ngrams)):
    _ngrams[i] = FreqDist()
_ngrams = tuple(_ngrams)
sentences = ('<s> the quick brown fox </s>', '<s> the quick brown cat </s>')
for s in sentences:
    _logger.log(9,"Adding sentence: '{}{}'.".format(s[:50],'..' if len(s) > 50 else ''))
    tokens = s.split(' ')
    for i in range(1,_max_order+1):
        _ngrams[i].update(ngrams(tokens,i))


for ngram in _ngrams[_max_order]:
    print('{}: {}'.format(ngram, kn(ngram)))

# # run some tests, probabilities must decrease or stay the same!
p = kn('quick brown fox'.split(), d=.7); print(p)
p_ = kn('slow brown fox'.split(), d=.7); print(p_); assert p >= p_; p = p_

p_ = kn('quick black fox'.split(), d=.7); print(p_); assert p >= p_; p = p_
p_ = kn('slow black fox'.split(), d=.7); print(p_); assert p >= p_; p = p_

p_ = kn('quick brown dog'.split(), d=.7); print(p_); assert p >= p_; p = p_
p_ = kn('slow brown dog'.split(), d=.7); print(p_); assert p >= p_; p = p_
p_ = kn('quick black dog'.split(), d=.7); print(p_); assert p >= p_; p = p_
p_ = kn('slow black dog'.split(), d=.7); print(p_); assert p >= p_; p = p_

print('====')

p = kn('quick brown fox'.split(), d=.1); print(p)
p_ = kn('slow brown fox'.split(), d=.1); print(p_); assert p >= p_; p = p_

p_ = kn('quick black fox'.split(), d=.1); print(p_); assert p >= p_; p = p_
p_ = kn('slow black fox'.split(), d=.1); print(p_); assert p >= p_; p = p_

p_ = kn('quick brown dog'.split(), d=.1); print(p_); assert p >= p_; p = p_
p_ = kn('slow brown dog'.split(), d=.1); print(p_); assert p >= p_; p = p_
p_ = kn('quick black dog'.split(), d=.1); print(p_); assert p >= p_; p = p_
p_ = kn('slow black dog'.split(), d=.1); print(p_); assert p >= p_; p = p_

print('====')
p = kn('<s> the quick'.split(), d=.7); print(p)
p = kn('the quick brown'.split(), d=.7); print(p)
p = kn('quick brown fox'.split(), d=.7); print(p)
p = kn('quick brown cat'.split(), d=.7); print(p)
p = kn('brown cat </s>'.split(), d=.7); print(p)
p = kn('brown fox </s>'.split(), d=.7); print(p)


# predict_sentence()

# print('====')

# from nltk.book import text1
# from nltk import tokenize

# for sentence in tokenize.sent_tokenize(' '.join(text1.tokens)):
#     s = ['<s>'] + tokenize.word_tokenize(sentence) + ['</s>']
#     _logger.debug("Adding sentence: '{}{}'.".format(s[:10],'..' if len(s) > 50 else ''))
#     for i in range(1,_max_order+1):
#         _ngrams[i].update(ngrams(s,i))

# print('====')

# p = kn('quick brown fox'.split(), d=.7); print(p)
# p_ = kn('slow brown fox'.split(), d=.7); print(p_)

# p_ = kn('quick black fox'.split(), d=.7); print(p_)
# p_ = kn('slow black fox'.split(), d=.7); print(p_)

# p_ = kn('quick brown dog'.split(), d=.7); print(p_)
# p_ = kn('slow brown dog'.split(), d=.7); print(p_)
# p_ = kn('quick black dog'.split(), d=.7); print(p_)
# p_ = kn('slow black dog'.split(), d=.7); print(p_)

# print('====')

# p = kn('quick brown fox'.split(), d=.1); print(p)
# p_ = kn('slow brown fox'.split(), d=.1); print(p_)

# p_ = kn('quick black fox'.split(), d=.1); print(p_)
# p_ = kn('slow black fox'.split(), d=.1); print(p_)

# p_ = kn('quick brown dog'.split(), d=.1); print(p_)
# p_ = kn('slow brown dog'.split(), d=.1); print(p_)
# p_ = kn('quick black dog'.split(), d=.1); print(p_)
# p_ = kn('slow black dog'.split(), d=.1); print(p_)

# print('predicting sentence')
# predict_sentence(max_length=30,num_top_words=100,num_skip_words=100)

