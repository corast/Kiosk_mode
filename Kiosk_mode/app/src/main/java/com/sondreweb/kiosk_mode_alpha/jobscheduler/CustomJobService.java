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
            if(job.getTag().equals(CustomContentProvider.jobTag)){
                //TODO gjør syncingen, siden vi vet at vi er på WIFI og enheten lader.
                //Gjør denne syncingen på en tråd også.

                //Hent all data fra databasen og send dette til databasen, når vi får en godkjent respons fra databasen eller hva den nå sender i respons.
                //Kan vi slette all data fra databasen som vi nettop sendte over.
                Log.d(TAG,"Schedulerer starter job: "+job.toString());
            }
        return false; //Trenger ikke fullføre arbeidet på en tråd, vi kan fikse dette selv.
    }

    //skal aldri stoppe en Job uansett.
    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

}
