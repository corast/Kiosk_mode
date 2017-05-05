package com.sondreweb.kiosk_mode_alpha.jobscheduler;

import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.activities.AdminPanelActivity;
import com.sondreweb.kiosk_mode_alpha.application.ApplicationController;
import com.sondreweb.kiosk_mode_alpha.classes.GeofenceClass;
import com.sondreweb.kiosk_mode_alpha.storage.CustomContentProvider;
import com.sondreweb.kiosk_mode_alpha.storage.GeofenceTable;
import com.sondreweb.kiosk_mode_alpha.storage.KioskDbContract;
import com.sondreweb.kiosk_mode_alpha.storage.SQLiteHelper;
import com.sondreweb.kiosk_mode_alpha.storage.StatisticsTable;
import com.sondreweb.kiosk_mode_alpha.utils.AppUtils;
import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.HashMap;
import java.util.Map;
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
                    synchStatisticsNow();
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

    private void synchStatisticsNow() {

        ApplicationController controller = new ApplicationController(); //Lager en instance av controlleren vår.
        final String URLRequest = PreferenceUtils.getSynchStatisticsUrl(getApplicationContext());

        SQLiteHelper sqLiteHelper = SQLiteHelper.getInstance(getApplicationContext());
        ArrayList<ContentValues> list = sqLiteHelper.getAllStatistics();
        if(list.isEmpty()){//Kan bare returnere
            return;
        }

        final JSONObject statistics = new JSONObject(); //JSON objectet vi sender

        final JSONArray statisticsArray = new JSONArray();//JSON Array for statistikken.

        JSONObject object;//Brukes for å holde på verdiene og lage hver rad i jsonArrayet.
        for (ContentValues contentValue:list) { //dersom listen er tom, kjører denne aldri.
            try {
                object = new JSONObject();
                object.put("navn", contentValue.getAsString(KioskDbContract.Statistics.COLUMN_MONUMENT));
                object.put("besokId", contentValue.getAsString(KioskDbContract.Statistics.COLUMN_VISITOR_ID));
                object.put("dato", contentValue.getAsString(KioskDbContract.Statistics.COLUMN_DATE));
                object.put("tid", contentValue.getAsString(KioskDbContract.Statistics.COLUMN_TIME));
                statisticsArray.put(object);
            }catch (JSONException e){
                Log.e(TAG,e.getMessage());
            }
        }

        try {
            statistics.put("statistics", statisticsArray);
        }catch (JSONException e){
            Log.e(TAG,e.getMessage());
        }

        /*Lager et JsonRequest fra Volley biblioteket. Hvor vi sender et JSONobject og motar et JSONobject fra server.
        *  statistics: JSONobjectet vi sender med, URLRequest serveren sin URL med script eller hva det nå er som tar imot.
        * */
        final JsonObjectRequest req = new JsonObjectRequest(URLRequest, statistics,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //TODO: Håndtere success respons og tømme data fra enheten.
                        try {
                            Log.d(TAG,"onRespons()");
                            Log.d(TAG,"getString Stat:"+response.getString("stat"));
                            //Log.d(TAG, response.getJSONObject("resposn").toString());
                            VolleyLog.v("Response:%n %s", response.toString(4));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.e(TAG,error.toString());
                VolleyLog.e("Error: ", error.getMessage());
            }
            /**
             * Passing some request headers
             * */
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        });

        //Bruker controlleren og legger Requesten vi lagde for å sende til servern i køen på systemet.
        ApplicationController.getInstance().addToRequestQueue(req);
    }

    //skal aldri stoppe en Job uansett. Men noen tilfeller kan det være greit å stoppe en job som er i gang.
    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

/*
*   Gamle metoden for nettverkskommunikasjon, ble lagd før vi implementerte Volley, men funger forsatt så.
*
*   Seperat tråd som laster ned innhold fra en server og laster inn i databasen vår for Geofence.
* */
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
                             * vi bryr oss ikke om list[0] side vi lager vår egen med SQLite tabellen.
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

            if(AppUtils.DEBUG){//Debugg kode for testing.
                Log.d(TAG,"contentValues:"+ contentValues.toString());
            }
            //TODO: da skal contenValues være full med geofence.

            //Vi skal da bytte ut alle geofence med disse dersom vi faktisk fikk tilbake noen.
            if(!contentValues.isEmpty()){
                //Oppdatere sist synk tid, slik av vi kan se senere når vi faktisk gjorde denne synkroniseringen.
                PreferenceUtils.updatePrefLastSynchronizeGeofence(getApplicationContext());
                //Erstatter alle Geofencene med disse nye vi mottok.
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
