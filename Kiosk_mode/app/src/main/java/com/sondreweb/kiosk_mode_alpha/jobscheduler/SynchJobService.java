package com.sondreweb.kiosk_mode_alpha.jobscheduler;

import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.activities.AdminPanelActivity;
import com.sondreweb.kiosk_mode_alpha.classes.GeofenceClass;
import com.sondreweb.kiosk_mode_alpha.storage.CustomContentProvider;
import com.sondreweb.kiosk_mode_alpha.storage.GeofenceTable;
import com.sondreweb.kiosk_mode_alpha.storage.KioskDbContract;
import com.sondreweb.kiosk_mode_alpha.storage.SQLiteHelper;
import com.sondreweb.kiosk_mode_alpha.storage.StatisticsTable;
import com.sondreweb.kiosk_mode_alpha.utils.AppUtils;
import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by sondre on 11-Apr-17.
 *
 * Ansvar for å starte Syncing ved riktig tidspunkt til serveren.
 * Slik at vi kan synce opp data når vi vet at enheten har tilstrekkelig med tid.
 */


public class SynchJobService extends JobService{

    private final static String TAG = SynchJobService.class.getSimpleName();

    //er 4 jobber vi kan gjøre, 1 Synch automatisk fra CustomContentProvider, 2 Synch manuelt Statistikken opp, 3 Synch manuelt geofencene.

    //Return verdien er på om resten av arbeiet skal foregå på en anne trår eller ikke
    @Override
    public boolean onStartJob(JobParameters job) {
        //TODO: Gjør sync basert på jobben.

        if( !SQLiteHelper.getInstance(getApplicationContext()).checkDataInStatisticsTable()){
            //Returnere true dersom det er noe data.
            return false;
        }
        switch (job.getTag()){
            //Automatisk oppsett av synchronisering for statistikken.
            case CustomContentProvider.synchJob:
                Log.d(TAG,"Schedulerer starter job: "+job.toString());

                if(SQLiteHelper.getInstance(getApplicationContext()).checkDataInStatisticsTable()){
                }
               break;

                //SYNCHJOB fra admin panelet.
            case AdminPanelActivity.synchStatisticsJob:
                //TODO: sync statistikken nå. Egentlig samme job som automatiske versjonen.
                Log.d(TAG,"starter job "+AdminPanelActivity.synchStatisticsJob);
                if(SQLiteHelper.getInstance(getApplicationContext()).checkDataInStatisticsTable()) {
                    //Gjør jobben...
                }
                break;
            case AdminPanelActivity.synchGeofenceJob:
                Log.d(TAG,"starter job"+ AdminPanelActivity.synchGeofenceJob);
                String URLRequest = PreferenceUtils.getSynchGeofenceUrl(getApplicationContext());
                Log.d(TAG, URLRequest);
                new DownloadGeofencesFromURL().execute(URLRequest); //henter geofence og gjør jobben.

                break;
        }
        return false; //Trenger ikke fullføre arbeidet på en tråd, vi kan fikse dette selv.
    }

    //skal aldri stoppe en Job uansett. Men noen tilfeller kan det være greit å stoppe en job som er i gang.
    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }


   private class DownloadGeofencesFromURL extends AsyncTask<String, String, String> {

        private final String dTag = DownloadGeofencesFromURL.class.getSimpleName();

        @Override
        protected String doInBackground(String... params) {
            ArrayList<ContentValues> contentValues = new ArrayList<>(); //en ArrayList som holder på noen verdier.
            if(AppUtils.checkIfNetwork(getApplicationContext())){
                Log.d(TAG, "Vi har nett");
                try{
                    ContentValues value;

                    URL url = new URL(params[0]);
                    //URLConnection urlConnection =  url.openConnection();
                    //urlConnection.connect();
                    BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));

                    String line;

                    while ((line = input.readLine()) != null){
                        value = new ContentValues();

                        String[] list = line.split(Pattern.quote(":"));
                        //Bare noe tomme linjer som kan ha kommet med.
                        if(list.length == 4){
                             /*
                             * vi bryr oss ikke om list[0]
                             * 2:59.1191633:11.398382:219
                             * nr:lat:long:radius
                             * */
                            value.put(GeofenceTable.COLUMN_LATITUDE, Double.parseDouble(list[1]));
                            value.put(GeofenceTable.COLUMN_LONGITUDE, Double.parseDouble(list[2]));
                            value.put(GeofenceTable.COLUMN_RADIUS, Integer.parseInt(list[3]));

                            contentValues.add(value); //Legger til denne ene.
                        }
                    }
                }catch (MalformedURLException e){
                    Log.e(dTag,"MalformedURL: "+e.getMessage());
                }catch (IOException e){
                    Log.e(dTag,"IOException: "+e.getMessage());
                }
            }
            if(AppUtils.DEBUG){
                Log.d(TAG,"contentValues:"+ contentValues.toString());
            }
            //TODO: da skal contenValues være full med geofence.

            //Vi skal da bytte ut alle geofence med disse dersom vi faktisk fikk tilbake noen.
            if(!contentValues.isEmpty()){
                Log.d(TAG,"contentValues ikke tom");
                PreferenceUtils.updatePrefLastSynchronizeGeofence(getApplicationContext());

                SQLiteHelper.getInstance(getApplicationContext()).replaceGeofences(contentValues);

            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            //TODO: splitt opp alt sammen.
            Toast.makeText(getApplicationContext(),"Ferdig å synche Geofencene",Toast.LENGTH_LONG).show();
            AdminPanelActivity.staticUpdateGeofenceTable(getApplicationContext());
            super.onPostExecute(s);
            //TODO: be AdminPanelet oppdatere seg.
        }
   }
}
