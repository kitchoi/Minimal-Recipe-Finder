import requests,json
import math
import itertools

def get_json(request_result):
    ''' Read json data from the request results from Food2Fork
    Raise RuntimeError when the user query limit is reached
    Return: dictionary
    '''
    result = request_result.json()
    if 'error' in result:
        raise RuntimeError("Limit reached")
    else:
        return result


def find_recipes(key,preferred_ings,
                 title_include_kw=None,title_exclude_kw=None,
                 max_queries = 20, max_selected_ingredients=5):
    
    '''
    Input:
    key              - key for the Food2Fork API
    preferred_ings   - Preferred ingredients, a list of strings
                       (e.g. ["chicken","mushroom"])
    title_include_kw - The recipe title must contain at least 
                       one of these keywords
                       (default None, e.g. ["salad",])
    title_exclude_kw - The recipe title must not contain any of these keywords
                       (default None, e.g. ["soup",])
    max_queries      - integer
    
    Return: a list of dictionaries
    '''
    search_url= "http://food2fork.com/api/search?"
    get_url="http://food2fork.com/api/get?"
    preferred_ings = set(preferred_ings)
    if title_include_kw:
        title_include_kw = set([ kw.lower() for kw in title_include_kw])
    else:
        title_include_kw = set()
    if title_exclude_kw:
        title_exclude_kw = set([ kw.lower() for kw in title_exclude_kw])
    else:
        title_exclude_kw = set()
    recipes = []
    recipe_ids = {}
    nqueries = 0
    max_r = len(preferred_ings)
    
    def too_many_queries(nqueries,recipe_ids):
        return (nqueries + len(recipe_ids)) >= max_queries
    
    def next_ings_group(ings,max_r=None):
        if max_r:
            r = min(len(ings),max_r)
        else:
            r = len(ings)
        nCr_iter = itertools.combinations(ings,r)
        while r > 0:
            try:
                yield nCr_iter.next()
            except StopIteration:
                r -= 1
                nCr_iter = itertools.combinations(ings,r)
    
    # If there is no preferred ingredients,
    # search for everything
    if preferred_ings:
        iter_ings = itertools.combinations(preferred_ings,max_r)
        while max_r > 0 and not too_many_queries(nqueries,recipe_ids):
            try:
                item_subset = iter_ings.next()
            except StopIteration:
                if max_r > 0:
                    max_r -= 1
                    print "max_r:{}".format(max_r)
                    iter_ings = itertools.combinations(preferred_ings,max_r)
                    continue
                else:
                    break
            recipes = get_json(requests.get(
                search_url,
                params={'key':key,
                        'q':','.join(item_subset)}))['recipes']
            nqueries += 1
            # Matched recipes
            recipes = [ recipe for recipe in recipes 
                        if ( not title_include_kw or \
                        any([ kw in recipe['title'].lower() for kw in title_include_kw ])) and  \
                        all([ kw not in recipe['title'].lower() for kw in title_exclude_kw ])]
            if not recipes and max_r > max_selected_ingredients:
                max_r = max_r/2
                print "max_r:{}".format(max_r)
                iter_ings = itertools.combinations(preferred_ings,max_r)
            for recipe in recipes:
                recipe_ids["".join(recipe['source_url'].split("://")[1:])] = \
                        recipe['recipe_id']
                if too_many_queries(nqueries,recipe_ids): break
    else:
        recipes = get_json(requests.get(search_url,params={'key':key}))\
                  ['recipes'][:max_queries-1]
        nqueries = 1
        recipe_ids = dict([ ("".join(recipe['source_url'].split("://")[1:]),
                             recipe['recipe_id'])
                            for recipe in recipes ])
    
    print "Number of queries: {}+{}={}".format(nqueries,len(recipe_ids),
                                               nqueries+len(recipe_ids))
    recipe_details = [ get_json(requests.get(
        get_url,
        params={'key':key,'rId':rid}))['recipe'] 
                       for url,rid in recipe_ids.items() ]
    return recipe_details


def compute_recipe_score(recipes,preferred_ings,other_ings):
    score_recipe = []
    preferred_ings = set(preferred_ings)
    other_ings = set(other_ings)
    all_items = preferred_ings ^ other_ings
    for recipe in recipes:
        ingred = recipe['ingredients']
        ningredients = len(ingred)
        combined_ing = ' '.join(ingred).lower()
        nmatch_all,nmatch_preferred = map(sum,
                                          zip(*[ (item.lower() in combined_ing , 
                                                  item.lower() in preferred_ings)
                                                 for item in all_items ]))
        nmissing = max(0,ningredients - nmatch_all)
        score_recipe.append((nmissing,nmatch_all,recipe))
    return score_recipe



def pretty_print_ingredient(score_recipe):
    score1,score2,recipe = score_recipe
    print "{}, missing:{},utilise:{}".format(recipe['title'],
                                                  score1,
                                                  score2)
    for ing in recipe['ingredients']:
        print ing
    print "Source: {}".format(recipe['source_url'])
    print "Image: {}".format(recipe['image_url'])



preferred_ings = ['chicken','bacon','broccoli','carrot','potato']
other_ings = ['butter','flour','chicken stock','salt','sugar',
              'black pepper','cream','garlic','sherry','olive oil',
              'ginger','onion','white wine','tomato','cumin',
              'bell pepper','vinegar','rosemary','mustard',
              'shallot','milk','oregano','white pepper']
keys = ['a454746c462f4fb0c2de39a9f123c387',
        'c36331dcabc17bf5b28ace75a28bf4a6',
        'de437f508534184c3df4bb1400035739']

try:
    import cPickle
    results = cPickle.load(open('recipe_details.p'))
except IOError:
    results = find_recipes(keys[-1],
                           preferred_ings,
                           title_exclude_kw=['soup',],max_queries=30)

results = find_recipes(keys[-1],preferred_ings,max_queries=60)
scored_recipes = compute_recipe_score(results,preferred_ings,other_ings)
sorted_recipes = sorted(sorted(scored_recipes,key=lambda v: v[1],reverse=True),
                        key=lambda v: v[0])

printer = ( pretty_print_ingredient(s) for s in sorted_recipes )

