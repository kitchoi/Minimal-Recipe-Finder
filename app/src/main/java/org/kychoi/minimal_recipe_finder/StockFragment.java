package org.kychoi.minimal_recipe_finder;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.List;

/**
 * Created by kit on 12/22/14.
 */
public class StockFragment extends Fragment {
    /**
     * This fragment allows user to input a list of ingredients.
     * The ingredients are saved in a database and be used in the other fragments
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private View rootView;
    private IngredientDatabaseInterface dbInterface;
    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static StockFragment newInstance(int sectionNumber) {
        StockFragment fragment = new StockFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public StockFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_stock, container, false);
       // Button for adding an ingredient
        Button addIngredientButton = (Button) rootView.findViewById(R.id.addIngredientButton);
        addIngredientButton.setEnabled(false);

        // Add listener to enable or disable button
        EditText ing_name = (EditText) rootView.findViewById(R.id.ing_name);
        ing_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                check_form_enable_button(rootView);
            }
        });

        RadioGroup preferredRadioGroup = (RadioGroup) rootView.findViewById(R.id.preferredRadioGroup);
        preferredRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                check_form_enable_button(rootView);
            }
        });

        addIngredientButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view){
                // Ingredient Name
                EditText ing_name = (EditText) rootView.findViewById(R.id.ing_name);
                // Define the Ingredient instance
                Ingredient ing = new Ingredient();
                ing.setName(ing_name.getText().toString());
                // Need using soon?
                RadioButton preferredYes = (RadioButton) rootView.findViewById(R.id.preferredRadioYes);
                RadioButton preferredNo = (RadioButton) rootView.findViewById(R.id.preferredRadioNo);
                RadioGroup preferredRadioGroup = (RadioGroup) rootView.findViewById(R.id.preferredRadioGroup);
                int selectedId = preferredRadioGroup.getCheckedRadioButtonId();
                if (selectedId == preferredYes.getId()) {
                    ing.setPreferred(true);
                } else {
                    ing.setPreferred(false);
                }
                // Add ingredient to the Ingredient database
                long status = dbInterface.insertIngredient(ing);
                if (status != -1){
                    // Update ListView
                    ListView listView = (ListView) rootView.findViewById(R.id.stockListview);
                    ArrayAdapter<Ingredient> adapter = (ArrayAdapter<Ingredient>) listView.getAdapter();
                    adapter.add(ing);
                    adapter.notifyDataSetChanged();
                } else {
                    // Ingredient already exists
                    Toast.makeText(getActivity(), "Already have it", Toast.LENGTH_SHORT).show();
                }

                // Clear form
                ing_name.setText("");
                preferredRadioGroup.clearCheck();
            }
        });
        // Clear stock
        Button clearStockButton = (Button) rootView.findViewById(R.id.clearStockButton);
        clearStockButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view){
                dbInterface.deleteAllIngredients();
                ListView listView = (ListView) rootView.findViewById(R.id.stockListview);
                ArrayAdapter<Ingredient> adapter = (ArrayAdapter<Ingredient>) listView.getAdapter();
                adapter.clear();
                adapter.notifyDataSetChanged();
            }
        });
        dbInterface = new IngredientDatabaseInterface(getActivity());
        dbInterface.open();

        List<Ingredient> ingredients = dbInterface.getAllIngredients();
        // Set adapter for ListView
        ArrayAdapter<Ingredient> adapter = new ArrayAdapter<Ingredient>(getActivity(),
                android.R.layout.simple_list_item_1,ingredients);
        ListView listView = (ListView) rootView.findViewById(R.id.stockListview);
        listView.setAdapter(adapter);
        return rootView;
    }


    @Override
    public void onPause() {
        dbInterface.close();
        super.onPause();
    }

    @Override
    public void onResume() {
        dbInterface.open();
        super.onResume();
    }

    public static void check_form_enable_button(View rootView) {
        // Ingredient Name
        EditText ing_name = (EditText) rootView.findViewById(R.id.ing_name);
        // Need using soon?
        RadioGroup preferredRadioGroup = (RadioGroup) rootView.findViewById(R.id.preferredRadioGroup);
        // Button for adding an ingredient
        Button addIngredientButton = (Button) rootView.findViewById(R.id.addIngredientButton);
        if ( ing_name.getText().toString().length() > 0 &&  preferredRadioGroup.getCheckedRadioButtonId() > -1 ) {
            addIngredientButton.setEnabled(true);
        } else {
            addIngredientButton.setEnabled(false);
        }
    }
}
