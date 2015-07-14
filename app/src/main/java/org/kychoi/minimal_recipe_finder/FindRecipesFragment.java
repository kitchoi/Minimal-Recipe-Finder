package org.kychoi.minimal_recipe_finder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by kit on 12/22/14.
 */
public class FindRecipesFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private IngredientDatabaseInterface database;
    private RecipeFinder recipeFinder;
    public RequestQueue queue;
    public final List<Recipe> foundRecipes = new ArrayList<Recipe>();
    public final AtomicInteger nRequests = new AtomicInteger(0);

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static FindRecipesFragment newInstance(int sectionNumber) {
        FindRecipesFragment fragment = new FindRecipesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public FindRecipesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_findrecipes, container, false);
        database = new IngredientDatabaseInterface(getActivity());
        database.open();

        Button searchButton = (Button) rootView.findViewById(R.id.findRecipeButton);
        // Check if there is network connection
        searchButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isNetworkAvailable()) {
                    Toast.makeText(getActivity(), "No Network Connection",
                            Toast.LENGTH_LONG).show();
                } else {
                    // Clear the list of found recipe
                    foundRecipes.clear();

                    // Show Loading dialog
                    final ProgressDialog ringProgressDialog = ProgressDialog.show(getActivity(),
                            "","Loading...",true,true);
                    ringProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            clearSearch();
                            Toast.makeText(getActivity(),"Cancelled",Toast.LENGTH_SHORT).show();
                        }
                    });

                    // Find recipes
                    findRecipes();

                    CountDownTimer timer = new CountDownTimer(30000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            if (nRequests.get() == 0){
                                ringProgressDialog.dismiss();
                                // Update ListView
                                ListView listView = (ListView) rootView.findViewById(R.id.foundRecipesList);
                                ArrayAdapter<Recipe> adapter = (ArrayAdapter<Recipe>) listView.getAdapter();
                                adapter.notifyDataSetChanged();
                                TextView mTxtView = (TextView) rootView.findViewById(R.id.mText);
                                mTxtView.setText(nRequests.toString());
                            }
                        }
                        public void onFinish(){
                            ringProgressDialog.dismiss();
                            if (nRequests.get() > 0){
                                TextView mTxtView = (TextView) rootView.findViewById(R.id.mText);
                                mTxtView.setText(nRequests.toString());
                                clearSearch();
                                Toast.makeText(getActivity(),"Time out",Toast.LENGTH_SHORT).show();
                            }
                        }
                    }.start();
                }
            }
        });

        Button refreshButton = (Button) rootView.findViewById(R.id.refresh);
        // Check if there is network connection
        refreshButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Update ListView
                ListView listView = (ListView) rootView.findViewById(R.id.foundRecipesList);
                ArrayAdapter<Recipe> adapter = (ArrayAdapter<Recipe>) listView.getAdapter();
                adapter.notifyDataSetChanged();
                TextView mTxtView = (TextView) rootView.findViewById(R.id.mText);
                mTxtView.setText(nRequests.toString());
            }
        });

        RecipeRowAdapter adapter = new RecipeRowAdapter(getActivity(),foundRecipes,database);
        ListView listView = (ListView) rootView.findViewById(R.id.foundRecipesList);
        listView.setAdapter(adapter);

        queue = Volley.newRequestQueue(getActivity());
        recipeFinder = new RecipeFinder("Food2Fork","a454746c462f4fb0c2de39a9f123c387",
                                        getActivity(),rootView,
                                        queue,foundRecipes,nRequests);

        return rootView;
    }

    public void clearSearch(){
        queue.cancelAll(recipeFinder.apiName);
        nRequests.lazySet(0);
        foundRecipes.clear();
    }

    public void findRecipes(){
        List<String> ingredientNames = database.getIngredientNames(true,false);
        recipeFinder.query(ingredientNames);
    }

    /*@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }*/

    @Override
    public void onPause() {
        queue.stop();
        database.close();
        super.onPause();
    }

    @Override
    public void onResume() {
        database.open();
        queue.start();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        queue.cancelAll(recipeFinder.apiName);
        nRequests.lazySet(0);
        super.onDestroy();
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if no network is available networkInfo will be null
        // otherwise check if we are connected
        return networkInfo != null && networkInfo.isConnected();
    }

}
