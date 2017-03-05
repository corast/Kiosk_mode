package com.sondreweb.kiosk_mode_alpha.services;

/**
 * Created by sondre on 03-Mar-17.
 */

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.receivers.RestartBroadcastReciever;

import java.util.List;

/**
 * Created by sondre on 26-Jan-17.
 * The Service that will run in the background on a seperate thread(mostly)
 *  and do the work it is tasked with.
 *  Run events when the users reacts with a Geofence.
 *  Lock the device if the user exits the Geofences.
 *  Must be efficient enught, ie not draw all the battery power afther an hour etc.
 *
 *  Must be able to start itself up again when the system reboots, starts.
 *  Tasked with keeping the system only usable within the parameters(geofence).
 *  Tasked with Warning the user if he is almost exiting an Geofence. (need different Geofence levels).
 *
 *  Should be able to get the location with only GPS tracking.(this needs more reasearch).
 *
 * Servicen som skal gå i bakgrunn og har som oppgave
 * Oppgave:
 *      : Holde styr på mobilens posisjon.
 *      : Låse enheten viss grensene oversteges.
 *      : Gi beskjed til bruker om grensene(advarsler osl)
 */

/*
*   THREAD HANDELING
*   Activity.runOnUiThread(Runnable)
*   View.post(Runnable)
*   View.postDelayed(Runnable, long)
* */

    /*  TODO: Detect actions from system from broadcastReciever:
    *       android.intent.action.ACTION_SHUTDOWN
    *       android.intent.action.QUICKBOOT_POWEROFF
    *       BOOT_COMPLETED
     *        -> Starting av geofence når vi oppdater
    *
    * */

public class GeofenceTransitionService extends Service {

    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤Broadcast reviever TAGS¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤*/
    //Tags that we are listening for go here, to keep track of everyone this service is listening for.
    private static final String INTENT_FILTER_TAG = "com.sondreweb.GeoFencingAlpha.Activity.RestartGeofencing";

    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤END TAGS¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤*/


    private static final String TAG = GeofenceTransitionService.class.getSimpleName();

    private final int mId = 2222; //tilfeldig ID for Notifikasjonen.

    private NotificationCompat.Builder notificationBuilder;

    private NotificationManager notificationMananger;

    private final Handler handler = new Handler();//

    private int count = 0;

    public GeofenceTransitionService(Context applicationContext){//construktor to make an object of the class to reference too in MainActivity.
        super();
    }

    public GeofenceTransitionService(){} //default constructor


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");
        //locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        notificationMananger = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(this); //Instansiate the NotificatioBuilder.
        startInForeground("starting GeofenceService", true); //setter opp notifikasjonen
    }

    //Pending intents will start This again(i think).
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // we can also check wether the action is from the Geofence or simply starting up the service again.

        //this can create Error the first time this service is started. We will have to figure out what to do.
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        Log.d(TAG,"GeofenceEvent: "+geofencingEvent.toString());
        //Error handeling
        if( geofencingEvent.hasError() ){ //TODO:finne ut hvorfor det har oppstått en error, og fikse dette
            String errorMessage = getErrorString( geofencingEvent.getErrorCode() );
            //dersom geofenceEvent.getErrorCode() returnere GEOFENCE_NOT_AVAILABLE, betyr dette at brukeren har disabled network location provider.
            //Dermed vill alle registrerte Geofence bli fjernet og
            Log.e(TAG,errorMessage);
            onDestroy(); //We can destroy the service, because there is no geofence that it can monitor anymore, this needs to be fixed somehow.
        }

        //Retrieve GeofenceTransiton
        int geoFenceTransition = geofencingEvent.getGeofenceTransition();

        //check types
        if( geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT )
        {   //hent geofences som ble triggered
            List<Geofence> triggeredGeofence = geofencingEvent.getTriggeringGeofences();
            //Create a detail message with geofence recieved
            String geofenceTransitionDetails = getGeofenceTransitionDetails(geoFenceTransition, triggeredGeofence );
            //send notifikasjon detaljene som String
            startInForeground( geofenceTransitionDetails , true); //Update the details of the notification.
            //TODO: Lock the devise and/or warn the user.

            //TODO: 2 Geofences, one wich will warn the user, and the other one which will lock the device.
        }
        /*TODO sjekk hvilken av eventene fant sted, dersom det er ENTER, så vet vi at brukeren befinner seg innenfor området, og kan trygt fortsette(starte opp Monumentvandringen)
         dersom det er EXIT, så må vi sende en advarsem som varer i 5 minutter om at brukeren må gå tilbake innenfor specifisert område. Dersom EMTER evemten trer inn, stopper vi denne
         Tellingen. Men dersom ENTER ikke finner sted, så må vi låse enheten, slik at den ikke kan brukes til annet enn å finne veien tilbake.
        */ //TODO: Compass for navigasjon for brukeren, med en pil i retning mot sentrum av Geofencet. (Dette bør gå ganske greit).
        //startGeofencing();
        // startTimer();

        // TODO: start in foreground(to show notification that service is running to confirm).

        //handler.postDelayed(getGpsCoordinate, 1000); //1 sec delay ved oppstart av servicet.

        //RestartBroadcastReciever.completeWakefulIntent(intent);

        return START_STICKY; //When service is killed by the system, it will start up again.
    }


    private Runnable getGpsCoordinate = new Runnable() {
        @Override
        public void run() {

            count++;
            Log.i(TAG,"run time: "+count);

            //startInForeground();

            handler.postDelayed(this, 1000);// et sekund til neste itterasjon av tråd.
        }
    };


    //Generer detaljert melding om geofence
    private String getGeofenceTransitionDetails(int geoFenceTransition, List<Geofence> triggeredeGeofences)
    {
        //henter IDen til hver av geofencene.
        ArrayList<String> triggeredGeofenceList = new ArrayList<>();
        for (Geofence geofence : triggeredeGeofences ){
            triggeredGeofenceList.add( geofence.getRequestId() );
        }

        String status = null;
        if( geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER){
            status = "Entering";
        }else if( geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT ){
            status = "Exiting";
        }

        Log.d(TAG, "status: "+status + TextUtils.join(", ", triggeredGeofenceList));
        return status +" "+ TextUtils.join(", ", triggeredGeofenceList);
    }


    /* Notification build up:
    * | Icon | Title
    *         SubText
    * */

    private void startInForeground(String msg, boolean notify){ //vi må kalle denn viss vi skal oppdatere notifikasjonen.
        Log.d(TAG,"startInForeground()");
        //TODO: use input to change the notification.

        //TODO: Figure out what pending intent does here.

        notificationBuilder
                .setSmallIcon(getNotificationIcon()) //icon that the user see in status bar.
                .setContentTitle("Title") // Geofence Service
                .setContentText(msg) // Text; Location being monitored.
                .setSubText("SubText") //lan lot centrum geofence
                .setTicker("Service starting") //geofence running
                .setPriority(NotificationCompat.PRIORITY_MAX); //Makes the system prioritize this notification over the others(or the same as other with max

        startForeground(mId, notificationBuilder.build()); //Start showing the notification on the (Action)/task bar.
    }

    private int getNotificationIcon(){
        boolean useWhiteIcon = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
        //return useWhiteIcon ? R.drawable.visible_64 : R.drawable.visible_50;
        return R.drawable.visible_50;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        Intent broadcastIntent = new Intent(INTENT_FILTER_TAG);
        stopForeground(true);
        //handler.removeCallbacks(getGpsCoordinate); //fjerner tråden.
        // handler.notify(); //bare tester hva denne gjør.
        //sendBroadcast(broadcastIntent);
        //stopTimerTask();
    }

    //todo: asynctask for getting the locations.

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private String getErrorString(int errorCode)
    {
        switch (errorCode)
        {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "Geofence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many Geofences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending intents";
            default:
                return "Uknown error";
        }
    }
}
