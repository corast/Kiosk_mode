package com.sondreweb.kiosk_mode_alpha.jobscheduler;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.sondreweb.kiosk_mode_alpha.activities.AdminPanelActivity;
import com.sondreweb.kiosk_mode_alpha.storage.CustomContentProvider;
import com.sondreweb.kiosk_mode_alpha.storage.SQLiteHelper;
import com.sondreweb.kiosk_mode_alpha.storage.StatisticsTable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by sondre on 11-Apr-17.
 *
 * Ansvar for å starte Syncing ved riktig tidspunkt til serveren.
 * Slik at vi kan synce opp data når vi vet at enheten har tilstrekkelig med tid.
 */




public class SynchJobService extends JobService{

    private static final String ip = "54.69.169.144";


    private final static String TAG = SynchJobService.class.getSimpleName();

    //Return verdien er på om resten av arbeiet skal foregå på en anne trår eller ikke
    @Override
    public boolean onStartJob(JobParameters job) {
        //TODO: Gjør sync basert på jobben.

        if( !SQLiteHelper.getInstance(getApplicationContext()).checkDataInStatisticsTable()){
            //Returnere true dersom det er noe data.
            return false;
        }
        switch (job.getTag()){
            case CustomContentProvider.synchJob:
                Log.d(TAG,"Schedulerer starter job: "+job.toString());
                //TODO: Gjør synchronisering med serveren på en tråd, eller på main siden vi står i ladning og har wifi.
                //TODO: Sjekk at automatisk synching er satt.
                if(SQLiteHelper.getInstance(getApplicationContext()).checkDataInStatisticsTable()){


                }
                postStatisticsAsThread();

                SQLiteHelper sqLiteHelper = SQLiteHelper.getInstance(getApplicationContext());
                ArrayList<ContentValues> list = sqLiteHelper.getAllStatistics();
                for (ContentValues contentValue : list) {
                    PostStatistics(contentValue);
                }
               break;

            case AdminPanelActivity.synchJob:

                break;
        }
        return false; //Trenger ikke fullføre arbeidet på en tråd, vi kan fikse dette selv.
    }

    //skal aldri stoppe en Job uansett. Men noen tilfeller kan det være greit å stoppe en job som er i gang.
    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

    private void postStatisticsAsThread(){
        Thread backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                SQLiteHelper sqLiteHelper = SQLiteHelper.getInstance(getApplicationContext());
                ArrayList<ContentValues> list = sqLiteHelper.getAllStatistics();

                try{
                    URL url = new URL(ip+"/statistikk.php");
                }catch (Throwable t){

                }


                for (ContentValues contentValue : list) {
                    PostStatistics(contentValue);
                }
            }
        });

    }


    public void PostStatistics(ContentValues contentValues){


        String monument = contentValues.getAsString(StatisticsTable.COLUMN_MONUMENT);
        String visitor = contentValues.getAsString(StatisticsTable.COLUMN_VISITOR_ID);
        String date = contentValues.getAsString(StatisticsTable.COLUMN_DATE);
        String time = contentValues.getAsString(StatisticsTable.COLUMN_TIME);

        String text = "";
        BufferedReader reader=null;
        try {
            String data = URLEncoder.encode("navn", "UTF-8")
                    + "=" + URLEncoder.encode(monument, "UTF-8");

            data += "&" + URLEncoder.encode("besoksId", "UTF-8") + "="
                    + URLEncoder.encode(visitor, "UTF-8");

            data += "&" + URLEncoder.encode("dato", "UTF-8")
                    + "=" + URLEncoder.encode(date, "UTF-8");

            data += "&" + URLEncoder.encode("tid", "UTF-8")
                    + "=" + URLEncoder.encode(time, "UTF-8");

            //send data
            URL url = new URL(ip+"/statistikk.php");
            //send post
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write( data );
            wr.flush();

            //server response
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;

            // Read Server Response
            while((line = reader.readLine()) != null)
            {
                // Append server response in string
                sb.append(line + "\n");
            }


            text = sb.toString();


        } catch(Exception ex) {

        }
        finally
        {
            try
            {

                reader.close();
            }

            catch(Exception ex) {}
        }

        // Show response on activity
        Toast.makeText(getApplicationContext(),text,Toast.LENGTH_SHORT).show();
        }

}
