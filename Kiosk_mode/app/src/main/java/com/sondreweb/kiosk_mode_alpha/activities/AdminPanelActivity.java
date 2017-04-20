package com.sondreweb.kiosk_mode_alpha.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
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
import com.sondreweb.kiosk_mode_alpha.jobscheduler.CustomJobService;
import com.sondreweb.kiosk_mode_alpha.services.GeofenceTransitionService;
import com.sondreweb.kiosk_mode_alpha.storage.KioskDbContract;
import com.sondreweb.kiosk_mode_alpha.storage.SQLiteHelper;
import com.sondreweb.kiosk_mode_alpha.storage.StatisticsTable;
import com.sondreweb.kiosk_mode_alpha.utils.AppUtils;
import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by sondre on 14-Apr-17.
 */

public class AdminPanelActivity extends AppCompatActivity {

    private static final String TAG = AdminPanelActivity.class.getSimpleName();

    Button kiosk_button,button_schedule_sync;
    TextView statistics_text;
    EditText edit_text_pref_kiosk;

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
        button_schedule_sync = (Button) findViewById(R.id.button_schedule_sync);


        statistics_text = (TextView) findViewById(R.id.text_view_content_provider_test);

        //TODO: lage en edit text.
        edit_text_pref_kiosk = (EditText) findViewById(R.id.edit_text_pref_kiosk_mode);
        edit_text_pref_kiosk.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        updateGui();
    }

    @Override
    protected void onStart() {
        /*
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
        } */
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish(); //Ber systemet fjerne denne activiteten fra stacken osv.
    }

    @Override
    protected void onStop() {

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

        //Dersom ingen til å synchronisere.
        if(SQLiteHelper.getInstance(getApplicationContext()).checkDataInStatisticsTable()){
            button_schedule_sync.setText(getResources().getString(R.string.admin_panel_synchronize));
            button_schedule_sync.setClickable(true);

        }else{
            button_schedule_sync.setText(getResources().getString(R.string.admin_panel_synchronize_empty_database));
            button_schedule_sync.setClickable(false);
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
                Toast.makeText(getApplicationContext(),app + "Settes nå som kiosk mode appliasjon", Toast.LENGTH_SHORT).show();
                PreferenceUtils.setPrefkioskModeApp(app,getApplicationContext());

            }else{
                Toast.makeText(getApplicationContext(),app + " er ikke innstallert", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void checkIfDataInStatisticsTable(View view){
       if(SQLiteHelper.getInstance(getApplicationContext()).checkDataInStatisticsTable()){
            //Det er data i databasen med statistikk.

       }else{
           Toast.makeText(getApplicationContext(), "Det er Ikke Statistikk tilgjengelig", Toast.LENGTH_SHORT).show();
       }
    }

    public final static String syncKey = "com.firebase.sync.key";
    private final static String syncValue = "com.firebase.sync.value";
    public final static String jobTag = "SYNC_WITH_DATABASE";

    public void scheduleSync(View view){
        if(SQLiteHelper.getInstance(getApplicationContext()).checkDataInStatisticsTable()){
            //scheduleJobNow();
            //TODO: forandre på teksten på knappen?
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.admin_panel_synchronize), Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.admin_panel_synchronize_empty_database), Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleJobNow(){
        //TODO: schedule synchronize. Krever forsatt wifi, men trenger ikke å være ladd.
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(getApplicationContext()));

        Bundle myExtrasBundle = new Bundle();

        myExtrasBundle.putString(syncKey,syncValue);

        Job myJob = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(CustomJobService.class)

                // uniquely identifies the job
                .setTag(jobTag)

                // one-off job
                .setRecurring(false)

                // persist past a device reboot
                .setLifetime(Lifetime.FOREVER)

                // start between 0 and 120 seconds from now after constraints met.
                .setTrigger(Trigger.executionWindow(0, 20))

                // overwrite an existing job with the same tag
                .setReplaceCurrent(true)

                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)

                // constraints that need to be satisfied for the job to run
                .setConstraints(
                        // only run on an unmetered network, i vårt tilfelle Ikke mobilnett som koster penger.
                        Constraint.ON_UNMETERED_NETWORK
                )
                .setExtras(myExtrasBundle)
                .build();

        dispatcher.mustSchedule(myJob);
        dispatcher.schedule(myJob);
    }

    /*
    *   Test metoder.
    * */
    public void insertDataTest(View view){
        //TODO contentResolver til å inserte en rad.
        //Må lage et monument tall
        //Må lage en random id
        //Må lage en dato
        //Må lage en tid

        //int monumentId = 2;
        Random rn = new Random();

        int monumentId = rn.nextInt(6); //fra 0-4  (min-max)+1 -> [min, max]
        int date = rn.nextInt(200)+1; //fra 1-200
        int time = rn.nextInt(10000)+1; //Fra 1 ms til 10000
        int visitor_id = rn.nextInt(10000)+1; //Fra 1 ms til 10000

        ContentValues contentValue = new ContentValues();

        contentValue.put(StatisticsTable.COLUMN_MONUMENT, monumentId);
        contentValue.put(StatisticsTable.COLUMN_DATE, date);
        contentValue.put(StatisticsTable.COLUMN_TIME, time);
        contentValue.put(StatisticsTable.COLUMN_VISITOR_ID,visitor_id);

        Uri uri = getContentResolver().insert(KioskDbContract.Statistics.CONTENT_URI, contentValue);
        Log.d(TAG,"Uri: "+uri);
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
            values.put(StatisticsTable.COLUMN_MONUMENT, monumentId);
            values.put(StatisticsTable.COLUMN_DATE, date);
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
