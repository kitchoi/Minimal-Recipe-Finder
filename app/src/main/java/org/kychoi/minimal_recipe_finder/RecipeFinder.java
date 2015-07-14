package org.kychoi.minimal_recipe_finder;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.text.Html;;


/**
 * Created by kit on 12/22/14.
 */
public class RecipeFinder {
    private String key;
    private int max_queries = 8;
    private Context parentActivity;
    private View rootView;
    private RequestQueue queue;
    private List<Recipe> foundRecipes;

    private AtomicInteger nRequests;

    public String apiName;

    private boolean too_many_queries(int nqueries, String[] recipe_ids) {
        return (nqueries + recipe_ids.length) >= max_queries;
    }

    public RecipeFinder(String apiName, String key, Context context, View rootView,
                        final RequestQueue queue, final List<Recipe> foundRecipes,
                        final AtomicInteger nRequests) {
        this.apiName = apiName;
        this.key = key;
        this.parentActivity = context;
        this.rootView = rootView;
        this.queue = queue;
        this.foundRecipes = foundRecipes;
        this.nRequests = nRequests;
    }

    public void setMax_queries(int max_queries) {
        this.max_queries = max_queries;
    }

    public void query(List<String> ingredients) {
        // Request a JSON response from the provided URL.
        JsonObjectRequest jsObjRequest;
        switch (apiName) {
            default:
                // Default Food2Fork
                Food2Fork_Handler api_handler = new Food2Fork_Handler(nRequests,max_queries);
                jsObjRequest = api_handler.jsObjRequest(parentActivity, ingredients, foundRecipes);
                break;
        }

        jsObjRequest.setTag(apiName);
        // Add the request to the RequestQueue.
        queue.add(jsObjRequest);
    }

    public Recipe loadRecipeIngredients(final Recipe recipe) {
        // Inquire and save ingredients to the given Recipe

        JsonObjectRequest jsObjRequest;
        switch (apiName) {
            default:
                // Default Food2Fork
                if (recipe.getRId() == null) {
                    Toast.makeText(parentActivity, "No ID found for recipe.", Toast.LENGTH_SHORT).show();
                    return recipe;
                }
                Food2Fork_Handler api_handler = new Food2Fork_Handler(nRequests,max_queries);
                jsObjRequest = api_handler.jsObjRequest(parentActivity,recipe);
                break;
        }

        jsObjRequest.setTag(apiName);
        // Add the request to the RequestQueue.
        queue.add(jsObjRequest);

        return recipe;
    }



    public class Food2Fork_Handler{
        public String search_url = "http://food2fork.com/api/search?";
        public String get_url = "http://food2fork.com/api/get?";
        public String apiName = "Food2Fork";
        public AtomicInteger nRequests;
        public int max_queries;

        public Food2Fork_Handler(AtomicInteger nRequests, int max_queries) {
            this.nRequests = nRequests;
            this.max_queries = max_queries;
        }

        public JsonObjectRequest jsObjRequest(Context parentActivity, List<String> ingredients,
                                              List<Recipe> foundRecipes){
            String url = search_url + String.format("key=%s&q=%s", key,
                                                    TextUtils.join(",", ingredients.toArray()));

            // Add 1 to the number of requests made
            nRequests.incrementAndGet();
            // Retrieve recipes
            JsonObjectRequest jsObjR = new JsonObjectRequest
                        (Request.Method.GET, url, null, recipeResponseListener(parentActivity, foundRecipes),
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                }
                            });
            return jsObjR;
        }


        public JsonObjectRequest jsObjRequest(Context parentActivity, Recipe recipe){
            String url = get_url + String.format("key=%s&rId=%s", key, recipe.getRId());
            // Add 1 to the number of requests made
            nRequests.incrementAndGet();
            // Read ingredients for each recipe
            JsonObjectRequest jsObjR = new JsonObjectRequest
                    (Request.Method.GET, url, null, ingredientResponseListener(parentActivity,recipe),
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                }
                            });

            return jsObjR;
        }

        public Listener<JSONObject> recipeResponseListener(final Context parentActivity,
                                                           final List<Recipe> foundRecipes){

            Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        JSONArray recipes = response.getJSONArray("recipes");
                        nRequests.decrementAndGet();
                        for (int i = 0; i < java.lang.Math.min(max_queries,recipes.length()); i++) {
                            // Load ingredients of the recipes
                            foundRecipes.add(loadRecipeIngredients(
                                    jsonToRecipe(recipes.getJSONObject(i))));
                        }
                    } catch (JSONException ex) {
                        if (response.has("error")) {
                            Toast.makeText(parentActivity, "Limit reached",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(parentActivity, response.toString(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
            };
            return listener;
        }

        public Listener<JSONObject> ingredientResponseListener(final Context parentActivity,
                                                               final Recipe recipe){
            Listener<JSONObject> listener = new Response.Listener<JSONObject>(){
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        nRequests.decrementAndGet();
                        JSONArray ingredients = response.getJSONObject("recipe").getJSONArray("ingredients");
                        for (int i = 0; i < ingredients.length(); i++) {
                            recipe.addIngredient(new Ingredient(ingredients.getString(i), 0));
                        }
                    } catch (JSONException ex) {
                        if (response.has("error")) {
                            Toast.makeText(parentActivity, "Limit reached",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            ex.printStackTrace();
                        }
                    }
                }
            };
            return listener;
        }

        public Recipe jsonToRecipe(JSONObject jsonObject) {
            final Recipe recipe = new Recipe();
            String title = "";
            String source_url = "";
            String rId = "";
            String image_url = "";
            try {
                title = Html.fromHtml(jsonObject.getString("title")).toString();
            } catch (JSONException ex) {
                Toast.makeText(parentActivity, "Error parsing title", Toast.LENGTH_SHORT).show();
            }
            try {
                source_url = jsonObject.getString("source_url");
            } catch (JSONException ex) {
                Toast.makeText(parentActivity, "Error parsing source_url",
                        Toast.LENGTH_SHORT).show();
            }
            try {
                rId = jsonObject.getString("recipe_id");
            } catch (JSONException ex) {
                Toast.makeText(parentActivity, "Error parsing recipe_id",
                        Toast.LENGTH_SHORT).show();
            }

            try {
                image_url = jsonObject.getString("image_url");
            } catch (JSONException ex) {
                Toast.makeText(parentActivity, "Error parsing recipe_id",
                        Toast.LENGTH_SHORT).show();
            }

            recipe.setTitle(title);
            recipe.setSource_url(source_url);
            recipe.setRId(rId);
            recipe.setImage_url(image_url);
            return recipe;
        }
    }
}
