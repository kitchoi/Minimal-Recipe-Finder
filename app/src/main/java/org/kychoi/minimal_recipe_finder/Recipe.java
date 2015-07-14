package org.kychoi.minimal_recipe_finder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by kit on 12/22/14.
 */
public class Recipe {
    private String title;
    private String source_url;
    private String publisher;
    private String image_url;
    private List<Ingredient> ingredients;
    private String rId;
    private Bitmap image;

    public Recipe() {
        this.ingredients = new ArrayList<Ingredient>();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSource_url(String source_url) {
        this.source_url = source_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public void setImage(Bitmap image) { this.image = image; }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public void setRId(String rId) {
        this.rId = rId;
    }

    public String getTitle() {
        return this.title;
    }

    public String getSource_url() {
        return this.source_url;
    }

    public String getImage_url() {
        return this.image_url;
    }

    public Bitmap getImage() {
        if ( this.image == null){
            new ImageDownloader(this).execute(this.getImage_url());
        }
        return this.image;
    }

    public String getPublisher() {
        return this.publisher;
    }

    public List<Ingredient> getIngredients() {
        return this.ingredients;
    }

    public String getRId() {
        return this.rId;
    }

    public void addIngredient(Ingredient ingredient) {
        this.ingredients.add(ingredient);
    }

    public String ingredientsToString() {
        // Return "ingredient1,ingredient2,..."
        String ingredients = "";
        Iterator<Ingredient> ing_iter = this.ingredients.iterator();
        while (ing_iter.hasNext()) {
            ingredients += ing_iter.next().getName();
            if (ing_iter.hasNext()) {
                ingredients += ",";
            }
        }
        return ingredients;
    }

    public int[] getNUtilised_nMissing(List<Ingredient> ingredients) {
        int[] n = new int[2];
        int nmatch = 0;
        String allIngredients = ingredientsToString();
        Iterator<Ingredient> ing_iter = ingredients.iterator();
        while (ing_iter.hasNext()) {
            if (allIngredients.toLowerCase().trim().contains(ing_iter.next().getName())) {
                nmatch += 1;
            }
        }
        n[0] = nmatch;
        n[1] = this.ingredients.size() - nmatch;
        return n;
    }

    public String toString() {
        String ing_string = ingredientsToString();
        if (ing_string.length() > 20) {
            return getTitle() + ":" + ingredientsToString().substring(0, 20);
        } else {
            return getTitle() + ":" + ingredientsToString();
        }
    }
}

class ImageDownloader extends AsyncTask<String, Void, Bitmap> {
    Recipe recipe;

    public ImageDownloader(Recipe recipe) {
        this.recipe = recipe;
    }

    protected Bitmap doInBackground(String... urls) {
        String url = urls[0];
        Bitmap mIcon = null;
        try {
            InputStream in = new java.net.URL(url).openStream();
            mIcon = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
        }
        return mIcon;
    }

    protected void onPostExecute(Bitmap result) {
        recipe.setImage(ThumbnailUtils.extractThumbnail(result, 150, 150));
    }
}
