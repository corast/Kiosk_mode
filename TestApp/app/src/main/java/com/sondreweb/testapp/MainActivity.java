package com.sondreweb.testapp;

import android.content.ContentValues;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {

        testInsertDatabase();
        super.onStart();
    }

    public void testInsertDatabase(){
        //int monumentId = 2;
        Random rn = new Random();

        String monumentId = Integer.toString(rn.nextInt(6)); //fra 0-4  (min-max)+1 -> [min, max]
        String date = "Test date";
        int time = rn.nextInt(10000)+1; //Fra 1 ms til 10000
        int visitor_id = rn.nextInt(10000)+1; //Fra 1 ms til 10000


        ContentValues contentValue = new ContentValues();

        contentValue.put(KioskDbContract.Statistics.COLUMN_MONUMENT, monumentId);
        contentValue.put(KioskDbContract.Statistics.COLUMN_DATE, date);
        contentValue.put(KioskDbContract.Statistics.COLUMN_TIME, time);
        contentValue.put(KioskDbContract.Statistics.COLUMN_VISITOR_ID,visitor_id);

        Uri uri = getContentResolver().insert(KioskDbContract.Statistics.CONTENT_URI, contentValue);

        //Log.d(TAG,"Uri: "+uri);
        //Uri yri = getContentResolver().bulkInsert()
        Toast.makeText(this, "New Statistics added", Toast.LENGTH_SHORT).show();
    }
}
