package com.sondreweb.kiosk_mode_alpha;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by sondre on 17-Feb-17.
 */

public class AppsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<ArrayList<AppModel>>{

    // This is the Adapter being used to display the list's data.
    AppAdapter adapter;

    public static final String TAG = AppsFragment.class.getSimpleName();

    public AppsFragment(){super();
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        // Give some text to display if there is no data.
        setEmptyText("No Applications");

        // Create an empty adapter we will use to display the loaded data.
        adapter = new AppAdapter(getActivity());
        setListAdapter(adapter);

        //start out with a progress indicator
        setListShown(false);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        //henter appen vi trkket på.
        AppModel app = (AppModel) getListAdapter().getItem(position);
        Log.d(TAG,"onListItemClick"+ app.getApplicationPackageName());
        if( app != null ){
            //lager et intent tilsvarende Appen vi trykket på.
            Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(app.getApplicationPackageName());

            if(intent != null){
                startActivity(intent);
            }
        }
    }

    @Override
    public Loader<ArrayList<AppModel>> onCreateLoader(int id, Bundle args) {
        Log.d(TAG,"Loader()");
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader with no arguments, so it is simple.
        //return new Loader<>(getActivity());
        return new AppsLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<AppModel>> loader, ArrayList<AppModel> data) {
        Log.d(TAG,"onLoadFinished()");
        //setter data inn i adapteret.
        adapter.setData(data);

        //listen burde vises
        if(isResumed()){
            setListShown(true);
        }else
        {
            setListShownNoAnimation(true);
        }


    }

    @Override
    public void onLoaderReset(Loader<ArrayList<AppModel>> loader) {
        Log.d(TAG, "onLoaderReset()");
        // Clear the data in the adapter.
        adapter.setData(null);
    }
}
