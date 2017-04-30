package com.sondreweb.kiosk_mode_alpha.activities;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.adapters.GeofenceAdapter;
import com.sondreweb.kiosk_mode_alpha.jobscheduler.SynchJobService;
import com.sondreweb.kiosk_mode_alpha.services.GeofenceTransitionService;
import com.sondreweb.kiosk_mode_alpha.settings.AdminSettingsActivity;
import com.sondreweb.kiosk_mode_alpha.storage.KioskDbContract;
import com.sondreweb.kiosk_mode_alpha.storage.SQLiteHelper;
import com.sondreweb.kiosk_mode_alpha.storage.StatisticsTable;
import com.sondreweb.kiosk_mode_alpha.utils.AppUtils;
import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
//import java.util.stream.Stream;

/**
 * Created by sondre on 14-Apr-17.
 * Activitete som vi logger inn til, som gir oss tilgang settings osv.
 *   Ansvar:
 *          vise Geofencene som er i bruk.
 *          Starte synchronisering med databasen for Geofence og å sende over statistikken,
 *              med enklere betingelser som kunn er WIFI. slik at vi kan sende med en gang WIFI er tilgjengelig.
 *          Skru av Kiosk mode på enheten, slik at vi kan bruke den til alminelig bruk igjen.
 *          Velge hvilken applikasjon som vi skal låse enheten til.
 */

public class AdminPanelActivity extends AppCompatActivity {

    private static final String TAG = AdminPanelActivity.class.getSimpleName();
    public static final String synchStatisticsJob = "sync_statistics_to_database";
    public static final String synchGeofenceJob = "sync_geofences_with_database";

    Toolbar toolbar;

    EditText edit_text_pref_kiosk;
    Button kiosk_button;

    Button button_schedule_sync_statistics;
    Button button_schedule_sync_geofence;

    TextView statistics_text;
    TextView textView_schedule_geofence;
    TextView textView_schedule_sync;

    TableLayout tableLayout;
    ListView geofenceListView;
    public static GeofenceAdapter geofenceAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        if(getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            Log.d(TAG, "orientation == landscape");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else{
            Log.d(TAG, "orientation != landscape");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        kiosk_button = (Button) findViewById(R.id.button_admin_panel_kiosk_mode);
        button_schedule_sync_statistics = (Button) findViewById(R.id.button_schedule_sync);

        statistics_text = (TextView) findViewById(R.id.text_view_content_provider_test);

        textView_schedule_geofence = (TextView) findViewById(R.id.text_schedule_geofence);


        //TODO: lage en edit text.
        edit_text_pref_kiosk = (EditText) findViewById(R.id.edit_text_pref_kiosk_mode);
        edit_text_pref_kiosk.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

        button_schedule_sync_geofence = (Button) findViewById(R.id.button_schedule_sync_geofence) ;

        textView_schedule_sync = (TextView) findViewById(R.id.text_schedule_statistics);

        geofenceListView = (ListView) findViewById(R.id.list_view_geofences);

        toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        if(toolbar != null) {
            getSupportActionBar().setTitle(R.string.admin_panel_toolbar_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            //toolbar.setTitleTextColor(ContextCompat.getColor(getApplicationContext(),R.color.redWard));

        }

        ViewStub stub = (ViewStub) findViewById(R.id.vs_continue_empty);
        geofenceListView.setEmptyView(stub);
        geofenceAdapter = new GeofenceAdapter(getApplicationContext());
        geofenceListView.setAdapter(geofenceAdapter);

        updateGui();

    }

    @Override
    protected void onStart() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean overlay = sharedPreferences.getBoolean(getResources().getString(R.string.KEY_SECURITY_GEOFENCE_OVERLAY),false);
        boolean test = sharedPreferences.getBoolean("android.settings.SYNC_SETTINGS",false); //funger ikke-
        Toast.makeText(getApplicationContext(),PreferenceUtils.getSynchGeofenceUrl(getApplicationContext()),Toast.LENGTH_SHORT).show();
        /*
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
        } */

        //tableLayout.addView();
        String textLastStatSync = getResources().getString(R.string.admin_panel_synchronize_text)
                .concat(": ")
                .concat(PreferenceUtils.getTimeSinceLastSynchronization(getApplicationContext()));
        textView_schedule_sync.setText(textLastStatSync);
        String textLastGeoSync = getResources().getString(R.string.admin_panel_synchronize_text)
                .concat(": ")
                .concat(PreferenceUtils.getPrefLastSynchroizeGeofence(getApplicationContext()));
        textView_schedule_geofence.setText(textLastGeoSync);

        updateGeofenceTable();
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_panel_toolbar,menu);


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int mId = item.getItemId();
        switch (mId){
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, AdminSettingsActivity.class);
                startActivity(settingsIntent);
                break;
            case R.id.action_statistics:
                Intent statisticsIntent = new Intent(this, StatisticsActivity.class);
                startActivity(statisticsIntent);
                break;
            case R.id.system_settings:
                startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS),0);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        Log.d(TAG,"onPause()");
        super.onPause();
        //finish(); //Ber systemet fjerne denne activiteten fra stacken osv.
    }

    @Override
    protected void onStop() {
        Log.d(TAG,"onStop()");
        super.onStop();
    }

    public void updateGui(){
        edit_text_pref_kiosk.setText(PreferenceUtils.getPrefkioskModeApp(getApplicationContext()));
        //bytter tekst på knapper osv.
        if(PreferenceUtils.isKioskModeActivated(getApplicationContext())){
            //True vill si at vi gjøre slik at knappen ikke er trykkbar.
            kiosk_button.setText(getResources().getString(R.string.admin_panel_kiosk_button_off));
            kiosk_button.setClickable(true);
            kiosk_button.setAlpha(1f);
        }else{
            kiosk_button.setText(getResources().getString(R.string.admin_panel_kiosk_button_on));
            kiosk_button.setClickable(false);
            kiosk_button.setAlpha(0.4f);
        }

        if(SQLiteHelper.getInstance(getApplicationContext()).checkDataInStatisticsTable()){
            //Det er noe å synchronizere, aktiver knappen.
            button_schedule_sync_statistics.setText(getResources().getString(R.string.admin_panel_synchronize_scheduled));
            button_schedule_sync_statistics.setClickable(true);
        }else{
            //Databasen er tom.
            button_schedule_sync_statistics.setText(getResources().getString(R.string.admin_panel_synchronize_empty_database));
            button_schedule_sync_statistics.setClickable(false);
        }

    }

    public void turnOffKioskMode(View view){
        //Setter dette til false.
        PreferenceUtils.setKioskModeActive(getApplicationContext(),false);
        Intent geofence_intent = new Intent(getApplicationContext(),GeofenceTransitionService.class);
        geofence_intent.setAction(GeofenceTransitionService.STOP_GEOFENCE_MONITORING);
        startService(geofence_intent);
        updateGui();
    }

    public void changeKioskApplication(View view){
        String app = null;
        try{
           app = edit_text_pref_kiosk.getText().toString();
        }catch (NullPointerException e){
            //vill bare si at det er tomt inni tekstfeltet.
        }

        if(app != null){
            if(AppUtils.isAppInstalled(getApplicationContext(),app)){
                Toast.makeText(getApplicationContext(),app + " Settes nå som kiosk mode appliasjon", Toast.LENGTH_SHORT).show();
                PreferenceUtils.setPrefkioskModeApp(app,getApplicationContext());

            }else{
                Toast.makeText(getApplicationContext(),app + " er ikke innstallert", Toast.LENGTH_SHORT).show();
            }
        }
    }

   static List GeofenceList = null;

    public void updateGeofenceTable(){

        if(!geofenceAdapter.isEmpty()){
            geofenceAdapter.clear();
        }

        GeofenceList = SQLiteHelper.getInstance(getApplicationContext()).getAllGeofencesClass();
        if(AppUtils.DEBUG){
            Log.d(TAG, GeofenceList.toString());
        }
        //TODO gå gjennom hele listen og skriv ut til tabell.
        geofenceAdapter.addAll(GeofenceList);
    }

    public static void staticUpdateGeofenceTable(Context context){
        if(GeofenceList != null) {
            GeofenceList = SQLiteHelper.getInstance(context).getAllGeofencesClass();
            geofenceAdapter.setData(GeofenceList);
            geofenceAdapter.notifyDataSetChanged();
        }
    }

    public final static String jobTag = "SYNC_WITH_DATABASE";

    //Når vi trykker på Schedune Synchronize statistics knappen
    public void scheduleStatisticsSync(View view){
        Log.d(TAG,"Data in statisticsTable: "+SQLiteHelper.getInstance(getApplicationContext()).checkDataInStatisticsTable());

        if(Patterns.WEB_URL.matcher(PreferenceUtils.getSynchStatisticsUrl(getApplicationContext())).matches()){
            if(SQLiteHelper.getInstance(getApplicationContext()).checkDataInStatisticsTable()){
                new RetrieveFeedTask().execute();
                //startStatisticsSync();
                //scheduleSynchStatisticsJobNow();
                //TODO: forandre på teksten på knappen?
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.admin_panel_synchronize_scheduled), Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.admin_panel_synchronize_empty_database), Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(getApplicationContext(),PreferenceUtils.getSynchGeofenceUrl(getApplicationContext())+"\n Not an valid URL",Toast.LENGTH_LONG);
        }

    }


    class RetrieveFeedTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        @Override
        protected String doInBackground(String... params) {

            SQLiteHelper sqLiteHelper = SQLiteHelper.getInstance(getApplicationContext());
            ArrayList<ContentValues> list = sqLiteHelper.getAllStatistics();

            for (ContentValues contentValue : list) {
                PostStatistics(contentValue);
            }

            return null;
        }

        protected void onPostExecute(String feed) {
            // TODO: check this.exception
            // TODO: do something with the feed
        }
    }
    public void startStatisticsSync(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                SQLiteHelper sqLiteHelper = SQLiteHelper.getInstance(getApplicationContext());
                ArrayList<ContentValues> list = sqLiteHelper.getAllStatistics();

                for (ContentValues contentValue : list) {
                    PostStatistics(contentValue);
                }
            }
        }).run();
    }

    private void PostStatistics(ContentValues contentValues){
        contentValues.toString();
        HttpURLConnection urlConnection = null; // Will do the connection
        BufferedReader reader;                  // Will receive the data from web
        String strJsonOut;                      // JSON to send to server
        try {
            //HttpURLConnection
            //HttpClient httpClient = new DefaultHttpClient();
            //(URL url = new URL(buildUri.toString().trim());
            URL url = new URL(PreferenceUtils.getSynchStatisticsUrl(getApplicationContext()));
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(0);
            urlConnection.setConnectTimeout(1500); // timeout of connection
            urlConnection.setRequestProperty("Content-Type", "text/plain; charset=utf-8"); // what format will you send
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true); // will receive
            urlConnection.setDoOutput(true); // will send
            urlConnection.connect();
            OutputStream outputStream = urlConnection.getOutputStream();
            OutputStreamWriter ow = new OutputStreamWriter(outputStream);
            //PHPObject
            JSONObject objLogin = new JSONObject();
            objLogin.put("navn",contentValues.getAsString(StatisticsTable.COLUMN_MONUMENT));
            objLogin.put("besoksId",contentValues.getAsString(StatisticsTable.COLUMN_VISITOR_ID));
            objLogin.put("dato",contentValues.getAsString(StatisticsTable.COLUMN_DATE));
            objLogin.put("tid",contentValues.getAsString(StatisticsTable.COLUMN_TIME));
            ow.write(objLogin.toString());
            ow.close();

//information sent by server
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {

                // response is empty, do someting
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String strJsonServer = buffer.toString();
            JSONObject objServerResponse = new JSONObject(strJsonServer);
            boolean status = objServerResponse.getBoolean("status");

            //return status; // Your response, do what you want.

        }catch (IOException e){
            Log.e(TAG, e.getMessage());
        }
        catch (JSONException e)
        {
            Log.e(TAG, e.getMessage());
            // error on the conversion or connection
        }
    }

    /*
    *   Starter når vi trykker på Schedune Synch Geofence knappen.
    * */
    public void sheduleGeofenceSync(View view){
        Toast.makeText(getApplicationContext(),"synchronize geofence with server", Toast.LENGTH_SHORT).show();
        //Noe som må sjekkes? Nope.
        if(Patterns.WEB_URL.matcher(PreferenceUtils.getSynchGeofenceUrl(getApplicationContext())).matches()){
            scheduleSynchGeofencesJobNow();
        }else{
            Toast.makeText(getApplicationContext(),PreferenceUtils.getSynchGeofenceUrl(getApplicationContext())+"\n Not an valid URL",Toast.LENGTH_LONG);
        }
    }

    private void scheduleSynchStatisticsJobNow(){
        //TODO: schedule synchronize. Krever forsatt wifi, men trenger ikke å være ladd.
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(getApplicationContext()));

        Job myJob = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(SynchJobService.class)

                // uniquely identifies the job
                .setTag(synchStatisticsJob)

                // one-off job
                .setRecurring(false)

                // persist past a device reboot
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)

                // start as quick as possible after trigger
                .setTrigger(Trigger.NOW)

                // overwrite an existing job with the same tag
                .setReplaceCurrent(true)

                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)

                // constraints that need to be satisfied for the job to run
                .setConstraints(
                        // only run on an unmetered network, i vårt tilfelle Ikke mobilnett som koster penger.
                        Constraint.ON_UNMETERED_NETWORK
                )
                .build();

        dispatcher.mustSchedule(myJob);
    }

    private void scheduleSynchGeofencesJobNow(){
        // Create a new dispatcher using the Google Play driver.
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(getApplicationContext()));

        Job myJob = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(SynchJobService.class)

                // uniquely identifies the job
                .setTag(synchGeofenceJob)

                // one-off job
                .setRecurring(false)

                // persist past a device reboot
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)

                // start as fast as avalible
                .setTrigger(Trigger.NOW)

                // overwrite an existing job with the same tag
                .setReplaceCurrent(true)

                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)

                // constraints that need to be satisfied for the job to run
                .setConstraints(
                        // only run on an unmetered network, i vårt tilfelle Ikke mobilnett som koster penger.
                        Constraint.ON_UNMETERED_NETWORK
                        // only run when the device is charging
                        //Constraint.DEVICE_CHARGING
                )
                .build();

        dispatcher.mustSchedule(myJob);
    }

    /*
    *   Test metoder til ContentProviceren, hvordan vi bruker denne.
    * */
    public void insertDataTest(View view){
        //TODO contentResolver til å inserte en rad.
        //Må lage et monument tall
        //Må lage en random id
        //Må lage en dato
        //Må lage en tid

        //int monumentId = 2;
        Random rn = new Random();

        String monumentId = Integer.toString(rn.nextInt(6)); //fra 0-4  (min-max)+1 -> [min, max]
        String date = Integer.toString(rn.nextInt(200)+1); //fra 1-200
        int time = rn.nextInt(10000)+1; //Fra 1 ms til 10000
        int visitor_id = rn.nextInt(10000)+1; //Fra 1 ms til 10000


        ContentValues contentValue = new ContentValues();

        contentValue.put(StatisticsTable.COLUMN_MONUMENT, monumentId);
        contentValue.put(StatisticsTable.COLUMN_DATE, date);
        contentValue.put(StatisticsTable.COLUMN_TIME, time);
        contentValue.put(StatisticsTable.COLUMN_VISITOR_ID,visitor_id);

        Uri uri = getContentResolver().insert(KioskDbContract.Statistics.CONTENT_URI, contentValue);

        //Log.d(TAG,"Uri: "+uri);
        //Uri yri = getContentResolver().bulkInsert()
        Toast.makeText(this, "New Statistics added", Toast.LENGTH_SHORT).show();
    }

    public void bulkInsertDataTest(View view){

        ArrayList<ContentValues> contentValuesArrayList = new ArrayList<>();
        ContentValues values;
        Random rn = new Random();
        int antall = rn.nextInt(20)+2; //2-21 tror jeg
        ContentValues[] contentValuesList = new ContentValues[antall];
        Log.d(TAG,"contentValueList.length "+ contentValuesList.length);


            //henter en random verdi fra 1-20
        for(int i = 0; i < antall;i++){
            values = new ContentValues();
            int monumentId = rn.nextInt(6); //fra 0-4  (min-max)+1 -> [min, max]
            int date = rn.nextInt(200)+1; //fra 1-200
            int time = rn.nextInt(10000)+1; //Fra 1 ms til 10000
            int visitor_id = rn.nextInt(10000)+1; //Fra 1 ms til 10000
            String dateS = "20/30/2017 20:30";

            values.put(StatisticsTable.COLUMN_MONUMENT, monumentId);
            values.put(StatisticsTable.COLUMN_DATE, dateS);
            values.put(StatisticsTable.COLUMN_TIME, time);
            values.put(StatisticsTable.COLUMN_VISITOR_ID,visitor_id);
            contentValuesList[i] = values;
        }

        //Log.d(TAG, contentValuesList)

        //Looper igjennom og lager en rekke med ContentValues.
        int i = getContentResolver().bulkInsert(KioskDbContract.Statistics.CONTENT_URI,contentValuesList);
    }

    public void lesDataTest(View view){
        //TODO contentResolver til å lese hele tabellen og skrive den ut.

        //Vi henter URI fra Contracten.
        Uri uri = KioskDbContract.Statistics.CONTENT_URI;

        String[] projection = new String[]{
                KioskDbContract.Statistics.COLUMN_MONUMENT,KioskDbContract.Statistics.COLUMN_VISITOR_ID,
                KioskDbContract.Statistics.COLUMN_DATE,KioskDbContract.Statistics.COLUMN_TIME
        };

        Cursor cursor = getContentResolver().query(uri, projection, null,null,null);

        statistics_text.setText("");
        if (cursor.moveToFirst()) {
            do {
                int monument_id = cursor.getInt(cursor.getColumnIndex(KioskDbContract.Statistics.COLUMN_MONUMENT));
                int visit_id = cursor.getInt(cursor.getColumnIndex(KioskDbContract.Statistics.COLUMN_VISITOR_ID));
                int date = cursor.getInt(cursor.getColumnIndex(KioskDbContract.Statistics.COLUMN_DATE));
                int time = cursor.getInt(cursor.getColumnIndex(KioskDbContract.Statistics.COLUMN_TIME));
                statistics_text.append("monumentId: " + monument_id + ", visitId: " + visit_id + ", date: " + date + ", time" + time + "\n");
            } while (cursor.moveToNext());
        }
        cursor.close(); //Lukker cursoren etter bruk.
    }
}
