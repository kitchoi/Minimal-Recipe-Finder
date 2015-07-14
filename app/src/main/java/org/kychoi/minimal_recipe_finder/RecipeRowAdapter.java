package org.kychoi.minimal_recipe_finder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Adapter for displaying the recipes found by RecipeFinder.java and
 * shown in FindRecipesFragment.java
 *
 * Created by kit on 1/18/15.
 */
public class RecipeRowAdapter extends ArrayAdapter<Recipe> {
    private Context context;
    private List<Recipe> recipes;
    private IngredientDatabaseInterface database;

    public RecipeRowAdapter(Context context, List<Recipe> recipes, IngredientDatabaseInterface database){
        super(context, R.layout.recipe_brief_layout, recipes);
        this.context = context;
        this.recipes = recipes;
        this.database = database;
    }

    static class ViewHolder {
        public TextView recipe_title;
        public ImageView recipe_image;
        public TextView recipe_info;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        // reuse views
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.recipe_brief_layout, parent, false);
            // configure view holder
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.recipe_title = (TextView) rowView.findViewById(R.id.recipe_row_name);
            viewHolder.recipe_image = (ImageView) rowView
                    .findViewById(R.id.recipe_row_image);
            viewHolder.recipe_info = (TextView) rowView.findViewById(R.id.recipe_row_info);
            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();

        Recipe recipe = recipes.get(position);
        // Show image for the recipe
        holder.recipe_image.setImageBitmap(recipe.getImage());

        holder.recipe_title.setText(recipe.getTitle());
        int[] nUtilised_nMissing = recipe.getNUtilised_nMissing(database.getAllIngredients());
        holder.recipe_info.setText("Utilise "+ Integer.toString(nUtilised_nMissing[0])
                                 + " of your stock.  Require " + Integer.toString(nUtilised_nMissing[1]) +" more.");

        return rowView;
    }
}
