package com.sondreweb.kiosk_mode_alpha.jobscheduler;

import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.sondreweb.kiosk_mode_alpha.storage.CustomContentProvider;

/**
 * Created by sondre on 11-Apr-17.
 *
 * Ansvar for å starte Syncing ved riktig tidspunkt til serveren.
 * Slik at vi kan synce opp data når vi vet at enheten har tilstrekkelig med tid.
 */


public class CustomJobService extends JobService{
    private final static String TAG = CustomJobService.class.getSimpleName();

    //Return verdien er på om resten av arbeiet skal foregå på en anne trår eller ikke
    @Override
    public boolean onStartJob(JobParameters job) {
        //TODO: Gjør sync basert på jobben.
        switch (job.getTag()){
            case CustomContentProvider.synchJob:
                //TODO: Gjør synchronisering med serveren på en tråd, eller på main siden vi står i ladning og har wifi.
                Log.d(TAG,"Schedulerer starter job: "+job.toString());
               break;
        }

        return false; //Trenger ikke fullføre arbeidet på en tråd, vi kan fikse dette selv.
    }

    //skal aldri stoppe en Job uansett. Men noen tilfeller kan det være greit å stoppe en job som er i gang.
    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

}
