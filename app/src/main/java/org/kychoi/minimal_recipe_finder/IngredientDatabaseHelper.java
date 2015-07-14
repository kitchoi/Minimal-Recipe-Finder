package org.kychoi.minimal_recipe_finder;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * Define the database for the ingredients the user has in stock
 * Created by kit on 12/21/14.
 */
public class IngredientDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ingredients.db";
    private static final int DATABASE_VERSION = 1;

    // Define the schema of the SQL database
    public static final String TABLE_ING = "Ingredient_List";
    public static final String COLUMN_NAME = "Ingredient";
    public static final String COLUMN_PREFERRED = "Preferred";
    private static final String DATABASE_CREATE = "create table "
            + TABLE_ING + "("
            + COLUMN_NAME + " text not null, "
            + COLUMN_PREFERRED + " int )";

    private String[] allColumns = { this.COLUMN_NAME, this.COLUMN_PREFERRED };

    public IngredientDatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    public String getName(Cursor cursor){
        return cursor.getString(0);
    }

    public int getPreferred(Cursor cursor){
        return cursor.getInt(1);
    }

    public boolean isPreferred(Cursor cursor){
        return getPreferred(cursor) == 1;
    }

    public String[] getAllColumns() {
        return this.allColumns;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(Ingredient.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_ING);
        onCreate(database);
    }

}
