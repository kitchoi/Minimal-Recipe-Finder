# coding: utf-8
import requests,json


search_url= "http://food2fork.com/api/search?"
get_url="http://food2fork.com/api/get?"
#key = 'a454746c462f4fb0c2de39a9f123c387'
#key = 'c36331dcabc17bf5b28ace75a28bf4a6'
key = 'de437f508534184c3df4bb1400035739'

# data = {'key':key,'q':'chicken thigh,potato,onion,celery'}
# r = requests.get(search_url,params=data)
# results = r.json()


# data = {'key':key,'rId':results['recipes'][0]['recipe_id']}
# r2 = requests.get(get_url,params=data)
# details = r2.json()
# ingredients = details['recipe']['ingredients']


# data = {'key':key,'rId':results['recipes'][3]['recipe_id']}
# r2 = requests.get(get_url,params=data)
# r2.json()['recipe']

# data = {'key':key,'rId':'16296'}
# r2 = requests.get(get_url,params=data)
# r2.json()['recipe']



import math

def nCr(n,r):
    f = math.factorial
    return f(n) / f(r) / f(n-r)


def nquery(N):
    rs = range(1,N+1)
    return sum([ nCr(N,r) for r in rs ])*2.


def get_json(request_result):
    result = request_result.json()
    if 'error' in result:
        raise RuntimeError("Limit reached")
    else:
        return result


import itertools
# Items need to be used
# Items need not be used up but available in kitchen (e.g. sugar, salt, butter)
# Keywords in title (optional)
items_to_use = set(['chicken','bacon','broccoli','carrot'])
items_always_have = set(['butter','flour','chicken stock','salt','sugar','black pepper','cream','garlic','sherry','olive oil','ginger'])
all_items = items_to_use ^ items_always_have
title_include_kw = set(['chicken','bacon','broccoli'])
title_exclude_kw = set(['soup',])
recipe_ids = {}
max_queries = 10
nqueries = 0

for r in xrange(len(items_to_use),0,-1):
    for item_subset in itertools.combinations(items_to_use,r):
        recipes = get_json(requests.get(
            search_url,
            params={'key':key,
                    'q':','.join(item_subset)}))['recipes']
        nqueries += 1
        for recipe in recipes:
            if (not title_include_kw or \
                any([ kw.lower() in recipe['title'].lower() \
                      for kw in title_include_kw ])) and  \
                all([ kw.lower() not in recipe['title'].lower() 
                      for kw in title_exclude_kw ]):
                source_url = "".join(recipe['source_url'].split("://")[1:])
                recipe_ids[source_url] = recipe['recipe_id']
    if len(recipe_ids) + nqueries > max_queries: break

nqueries = nqueries + len(recipe_ids)

recipe_details = [ get_json(requests.get(
    get_url,
    params={'key':key,'rId':rid}))['recipe'] 
                   for url,rid in recipe_ids.items() ]

score_recipe = []

for recipe in recipe_details:
    ingred = recipe['ingredients']
    ningredients = len(ingred)
    combined_ing = ' '.join(ingred)
    nmatch_all,nmatch_to_use = map(sum,zip(*[ (True, item in items_to_use) 
                                              for item in all_items 
                                              if item in combined_ing ]))
    match_score = float(nmatch_all)/float(ningredients)
    utilise_score = float(nmatch_to_use)/float(len(items_to_use))
    score_recipe.append((match_score,utilise_score,recipe))

sorted_list = sorted(score_recipe,key=lambda v:v[0]+v[1])


def pretty_print_ingredient(score_recipe):
    score1,score2,recipe = score_recipe
    print "{}, match:{:.0f}%,utilise:{:.0f}%".format(recipe['title'],
                                                     score1*100.,
                                                     score2*100.)
    for ing in recipe['ingredients']:
        print ing
    print "Image: {}".format(recipe['image_url'])



def next_ings_group(preferred_ings):
    r = len(preferred_ings)
    nCr_iter = itertools.combinations(preferred_ings,r)
    while r > 0:
        try:
            yield nCr_iter.next()
        except StopIteration:
            r -= 1
            nCr_iter = itertools.combinations(preferred_ings,r)

