package com.sondreweb.kiosk_mode_alpha.activities;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.jobscheduler.CustomJobService;
import com.sondreweb.kiosk_mode_alpha.storage.KioskDbContract;
import com.sondreweb.kiosk_mode_alpha.storage.StatisticsTable;
import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;

import java.util.Random;

/**
 * Created by sondre on 14-Apr-17.
 */

public class AdminPanelActivity extends AppCompatActivity {

    Button kiosk_button;
    TextView statistics_text;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);
        kiosk_button = (Button) findViewById(R.id.button_admin_panel_kiosk_mode);
        statistics_text = (TextView) findViewById(R.id.text_view_content_provider_test);
        //TODO: lage en edit text.

    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    public void turnOffKioskMode(View view){
        //Setter dette til false.
        PreferenceUtils.setKioskModeActive(getApplicationContext(),false);
    }

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

        ContentValues contentValues = new ContentValues();

        contentValues.put(StatisticsTable.COLUMN_MONUMENT, monumentId);
        contentValues.put(StatisticsTable.COLUMN_DATE, date);
        contentValues.put(StatisticsTable.COLUMN_TIME, time);
        contentValues.put(StatisticsTable.COLUMN_VISITOR_ID,visitor_id);

        Uri uri = getContentResolver().insert(KioskDbContract.Statistics.CONTENT_URI, contentValues);
        //Uri yri = getContentResolver().bulkInsert()
        Toast.makeText(this, "New Statistics added", Toast.LENGTH_SHORT).show();
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

        if(cursor.moveToFirst()){
            do{
                int monument_id = cursor.getInt(cursor.getColumnIndex(KioskDbContract.Statistics.COLUMN_MONUMENT));
                int visit_id = cursor.getInt(cursor.getColumnIndex(KioskDbContract.Statistics.COLUMN_VISITOR_ID));
                int date = cursor.getInt(cursor.getColumnIndex(KioskDbContract.Statistics.COLUMN_DATE));
                int time = cursor.getInt(cursor.getColumnIndex(KioskDbContract.Statistics.COLUMN_TIME));
                statistics_text.append("monumentId: "+monument_id+", visitId: "+visit_id+", date: "+date+", time"+time+"\n");
            }while(cursor.moveToNext());
        }

    }
}
