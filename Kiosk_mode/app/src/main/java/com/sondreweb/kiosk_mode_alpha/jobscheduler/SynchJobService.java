package com.sondreweb.kiosk_mode_alpha.jobscheduler;

import android.content.ContentValues;
import android.util.Log;
import android.widget.Toast;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.sondreweb.kiosk_mode_alpha.activities.AdminPanelActivity;
import com.sondreweb.kiosk_mode_alpha.storage.CustomContentProvider;
import com.sondreweb.kiosk_mode_alpha.storage.SQLiteHelper;
import com.sondreweb.kiosk_mode_alpha.storage.StatisticsTable;
import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
            case CustomContentProvider.synchJob: //Automatisk synching av statistikk.
                Log.d(TAG,"Schedulerer starter job: "+job.toString());
                //TODO: Gjør synchronisering med serveren på en tråd, eller på main siden vi står i ladning og har wifi.
                //TODO: Sjekk at automatisk synching er satt.
                if(SQLiteHelper.getInstance(getApplicationContext()).checkDataInStatisticsTable()){
                    postStatisticsAsThread();
                }

                SQLiteHelper sqLiteHelper = SQLiteHelper.getInstance(getApplicationContext());
                ArrayList<ContentValues> list = sqLiteHelper.getAllStatistics();
                for (ContentValues contentValue : list) {
                    PostStatistics(contentValue);
                }
               break;

                //SYNCHJOB fra admin panelet.
            case AdminPanelActivity.synchStatisticsJob:
                //TODO: sync statistikken nå. Egentlig samme job som automatiske versjonen.
                if(SQLiteHelper.getInstance(getApplicationContext()).checkDataInStatisticsTable()) {
                    //Gjør jobben...
                }
                break;
            case AdminPanelActivity.synchGeofenceJob:
                //TODO: synch geofencene ned fra databasen

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

            data += "&" + URLEncoder.encode("besoksId", "UTF-8")
                    + "=" + URLEncoder.encode(visitor, "UTF-8");

            data += "&" + URLEncoder.encode("dato", "UTF-8")
                    + "=" + URLEncoder.encode(date, "UTF-8");

            data += "&" + URLEncoder.encode("tid", "UTF-8")
                    + "=" + URLEncoder.encode(time, "UTF-8");

            //send data
            //URL url = new URL(ip+"/statistikk.php");
            URL url = new URL(PreferenceUtils.getSynchStatisticsUrl(getApplicationContext()));

            //send post
            URLConnection conn = url.openConnection();
            //Sets the value of the doOutput field for this URLConnection to the specified value.
            conn.setDoOutput(true);
            //endoder dataene med en Outputstream til connectionen.
            //conn.getOutputStream(): Returns an output stream that writes to this connection.
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

            //Skriver inn data.
            wr.write( data );
            wr.flush();

            //server response    getOutputStream(): Returns an input stream that reads from this open connection
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

            //TODO: update last synchronize.

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

    private void getGeofencesWithThread(){
        Thread backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try{
                    URL url = new URL(ip+"/geofence.php");
                }catch (Throwable t){

                }

            }
        });

    }
}
