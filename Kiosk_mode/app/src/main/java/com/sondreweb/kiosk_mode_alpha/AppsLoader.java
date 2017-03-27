package com.sondreweb.kiosk_mode_alpha;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by sondre on 17-Feb-17.
 * Custom loader, som loader alle innstallerte applikasjoner i en bakgrunnstråd, slik at UI(Main) tråden er ledig til andre ting.
 * TODO: Kunn legg til de vi ønsker at bruken kan ha tilgang til, som Monumentvandrings appen, og (kanskje) custom settings?
 *
 * source https://developer.android.com/reference/android/content/AsyncTaskLoader.html Under AppListLoader
 */

public class AppsLoader extends AsyncTaskLoader<ArrayList<AppModel>> {

    private static final String TAG = AppsLoader.class.getSimpleName();

    //listen over innstallerte apper i systemet.
    ArrayList<AppModel> innstalledApps;

    final PackageManager packageManager;

    public AppsLoader(Context context){
        super(context);

        // Retrieve the package manager for later use; note we don't
        // use 'context' directly but instead the save global application
        // context returned by getContext().
        packageManager = context.getPackageManager();
    }

    /**
     * This is where the bulk of our work is done.  This function is
     * called in a background thread and should generate a new set of
     * data to be published by the loader.
     */

    @Override
    public ArrayList<AppModel> loadInBackground() {
        //Retriev listen over installerte aplikasjoner.
        Log.d(TAG, "loadInBackground()");
                    //Flagget som sendes med PacketMangger har verdi 0, som er alle apper som har gitt tillatelse for å installeres.
        List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.PERMISSION_GRANTED);

        if( apps == null ){
            apps = new ArrayList<>();
        }

        final Context context = getContext();

        //Lag corresponderende apper og deres loadere
        //en ArrayList som vi fyller med de Appene som vil at brukene skal ha tilgang til.
        ArrayList<AppModel> items = new ArrayList<AppModel>();

        //går igjennom alle appene.
        for(int i = 0; i < apps.size(); i++){

            String packageName = apps.get(i).packageName;


            //kunn de som er lauchable/kjørbare intents vi er interresert i
            if(packageManager.getLaunchIntentForPackage(packageName) != null){

                    //tester med 3 apper som vi vill kanskje ha.
                    Log.d(TAG,"PackageName: " +packageName);
                if( packageName.equalsIgnoreCase("com.android.settings") || packageName.equalsIgnoreCase("com.sondreweb.geofencingalpha") || packageName.equalsIgnoreCase("com.android.chrome")
                        || packageName.equalsIgnoreCase("com.android.deskclock") || packageName.equalsIgnoreCase("com.android.gallery")){
                    Log.d(TAG,"Legg til app: "+ packageName);
                    AppModel app = new AppModel(context, apps.get(i));
                    app.loadLabel(context);

                    items.add(app);
                }
            }
        }

        //ArrayList<AppModel> allowedItems = new ArrayList<AppModel>();

        //sorterer listen med applikasjoner.
        Collections.sort(items,AppModel.ALPHA_COMPARATOR);

        return items;
    }



    /**
     * Handles a request to start the Loader.
     */

    @Override
    protected void onStartLoading() {
        Log.d(TAG,"onStartLoading()");
        if(innstalledApps != null){
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(innstalledApps);
        }

        if(takeContentChanged() || innstalledApps == null)
        {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        Log.d(TAG,"onStopLoading()");
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */

    @Override
    public void onCanceled(ArrayList<AppModel> apps) {
        Log.d(TAG,"onCanceled()");
        super.onCanceled(apps);

        // At this point we can release the resources associated with 'apps'
        // if needed.
        onReleaseResources(apps);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        Log.d(TAG, "onReset()");
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'innstalledApps'
        // if needed.
        if(innstalledApps != null){
            onReleaseResources(innstalledApps);
            innstalledApps = null;
        }
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */

    @Override
    public void deliverResult(ArrayList<AppModel> apps) {
        Log.d(TAG,"deliverResult()");
        if(isReset()){
            //An async query kom inn mens loaderen stoppet, vi trenger da ikke resultatet.
            if(apps !=null){
                onReleaseResources(apps);
            }
        }

        List<AppModel> oldApps = apps;
        innstalledApps = apps;

        if(isStarted()){
            super.deliverResult(apps);
        }

        if(oldApps != null){
            onReleaseResources(oldApps);
        }
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(List<AppModel> apps) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }


}
