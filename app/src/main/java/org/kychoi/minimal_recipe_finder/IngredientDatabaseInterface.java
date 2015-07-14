package org.kychoi.minimal_recipe_finder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kit on 12/22/14.
 */
public class IngredientDatabaseInterface {
    private SQLiteDatabase database;
    private IngredientDatabaseHelper dbHelper;

    public IngredientDatabaseInterface(Context context){
        dbHelper = new IngredientDatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }


    public boolean hasIngredient(String ingredient_name) {
        // Determine whether stock has an ingredient that matches the given name exactly
        ingredient_name = ingredient_name.toLowerCase();
        String[] columns = { dbHelper.COLUMN_NAME };
        Cursor cursor;
        cursor = database.query(dbHelper.TABLE_ING, columns,
                dbHelper.COLUMN_NAME + " = ?", new String[] {ingredient_name},
                null, null, null, "1");
        cursor.moveToFirst();
        if (!cursor.isAfterLast()){
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    public boolean hasSimilarIngredient(String ingredient_name){
        // Determine whether stock has an ingredient that matches the given name
        ingredient_name = ingredient_name.toLowerCase();
        String[] columns = { dbHelper.COLUMN_NAME };
        Cursor cursor = database.query(dbHelper.TABLE_ING,columns,
                    dbHelper.COLUMN_NAME + "LIKE ?",
                    new String[] { "%" + ingredient_name + "%"},
                    null,null,null,"1");
        cursor.moveToFirst();
        if (!cursor.isAfterLast()){
            // There is an ingredient in stock that contains the String ingredient_name
            cursor.close();
            return true;
        } else {
            cursor.close();
            // There is not an ingredient in stock that contains the String ingredient_name
            // But does the ingredient_name contains the name of any of the ingredients in stock?
            for (Ingredient stock_ing : getAllIngredients()){
                String stock_ing_name = stock_ing.getName().toLowerCase();
                if (ingredient_name.contains(stock_ing_name)){
                    return true;
                }
            }
        }
        return false;
    }

    public long insertIngredient(Ingredient ing){
        if (hasIngredient(ing.getName())){
            // Do not add ingredient that is already there
            return -1;
        } else {
            ContentValues values = new ContentValues();
            values.put(dbHelper.COLUMN_NAME, ing.getName());
            values.put(dbHelper.COLUMN_PREFERRED, ing.getPreferred());
            long insertID = database.insert(dbHelper.TABLE_ING, null, values);
            return insertID;
        }
    }

    public int deleteIngredient(Ingredient ing){
        return database.delete(dbHelper.TABLE_ING, dbHelper.COLUMN_NAME + " = " +
                DatabaseUtils.sqlEscapeString(ing.getName()), null);
    }

    public int deleteAllIngredients(){
        return database.delete(dbHelper.TABLE_ING, "1",null);
    }

    public List<Ingredient> getIngredients(boolean preferred, boolean other) {
        /* Return a list of Ingredient objects
           If preferred is true, return the ingredients that need to be used soon
           If other is true, return the ingredients that do not need to be used soon
           If both preferred and other are true, all ingredients are returned */
        List<Ingredient> allIng = new ArrayList<Ingredient>();
        Cursor cursor = database.query(dbHelper.TABLE_ING,
                dbHelper.getAllColumns(),null,null,null,null,null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Ingredient ing = cursorToIngredient(cursor);
            if ( ing.isPreferred() && preferred ) {
                allIng.add(ing);
            }
            if ( !ing.isPreferred() && other ){
                allIng.add(ing);
            }
            cursor.moveToNext();
        }
        cursor.close();
        return allIng;
    }

    public List<Ingredient> getAllIngredients(){
        return getIngredients(true,true);
    }

    public List<String> getIngredientNames(boolean preferred, boolean other) {
        // Similar to getIngredients but instead of getting a list of Ingredient objects
        // get a list of strings which are the names of the ingredients
        List<String> allIngName = new ArrayList<String>();
        List<Ingredient> allIng = getIngredients(preferred,other);
        for (int i=0; i < allIng.size(); i++){
            allIngName.add(allIng.get(i).getName());
        }
        return allIngName;
    }

    public Ingredient cursorToIngredient(Cursor cursor){
        // Return a new Ingredient object given a cursor to an entry
        Ingredient ing = new Ingredient();
        ing.setName(dbHelper.getName(cursor));
        ing.setPreferred(dbHelper.getPreferred(cursor));
        return ing;
    }

}
