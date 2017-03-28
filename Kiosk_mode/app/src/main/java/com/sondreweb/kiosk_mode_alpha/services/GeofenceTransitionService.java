package com.sondreweb.kiosk_mode_alpha.services;

/**
 * Created by sondre on 03-Mar-17.
 */

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import java.util.ArrayList;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.activities.HomeActivity;
import com.sondreweb.kiosk_mode_alpha.activities.LoginAdminActivity;
import com.sondreweb.kiosk_mode_alpha.receivers.RestartBroadcastReciever;
import com.sondreweb.kiosk_mode_alpha.settings.AdminPanel;
import com.sondreweb.kiosk_mode_alpha.utils.AppUtils;
import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;

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



public class GeofenceTransitionService extends Service implements
        LocationListener, FusedLocationProviderApi, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static final String STOP_KIOSK = "com.sondreweb.STOP_KIOSK";


    GoogleApiClient googleApiClient;

    /**
     *  LocationListener callbacks
     */

    @Override
    public void onLocationChanged(Location location) { //lytter til når location forandres, på denne måten kan vi lytte etter locationChange flere steder.

    }

    /*   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *   GoogleApiClient.ConnectionCallbacks
    *    ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    /*  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *   GoogleApiClient.OnConnectionFailedListener
    *   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤Broadcast reviever TAGS¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤*/
    //Tags that we are listening for go here, to keep track of everyone this service is listening for.
    private static final String INTENT_FILTER_TAG = "com.sondreweb.GeoFencingAlpha.Activity.RestartGeofencing";

    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤END TAGS¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤*/


    public static final String START_GEOFENCE = "comgeofence..start"
;
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

    private ScreenOffReceiver screenOffReceiver;

    private PowerManager.WakeLock wakeLock;

    static{

       // PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");
        //locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        notificationMananger = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(this); //Instansiate the NotificatioBuilder.
        startInForeground("starting GeofenceService", true); //setter opp notifikasjonen

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(STOP_KIOSK);
        screenOffReceiver = new ScreenOffReceiver();

        // Registrer Recievereren vi trenger for å fange SCREEN_OFF.
        registerReceiver(screenOffReceiver, filter);
    }

    //Pending intents will start This again(i think).
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG,"onStartCommand(intent, flag,startId) ##################################################");

        Log.d(TAG,intent.toString());
        // we can also check wether the action is from the Geofence or simply starting up the service again.
        if(intent.getAction().equalsIgnoreCase(START_GEOFENCE)){ //dersom vi starter servicen med hensikt å starte lokasjons håndtering
            Log.d(TAG,"Start LocationRequests fra servicen  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤");
            if(createGoogleApi()){

            }
        }
        //this can create Error the first time this service is started. We will have to figure out what to do.
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent); //henter Geofencet fra intent viss intent kommer fra et Geofence.
                //ellers så er det bare at vi starter Servicen fra hvilket som helst annet sted.
        if(geofencingEvent != null ){
            Log.d(TAG,"GeofenceEvent: "+geofencingEvent.toString());
            if( geofencingEvent.hasError() ){ //TODO:finne ut hvorfor det har oppstått en error, og fikse dette
                String errorMessage = getErrorString( geofencingEvent.getErrorCode() );
                //dersom geofenceEvent.getErrorCode() returnere GEOFENCE_NOT_AVAILABLE, betyr dette at brukeren har disabled network location provider.
                //Dermed vill alle registrerte Geofence bli fjernet og
                Log.e(TAG,errorMessage);
                onDestroy(); //We can destroy the service, because there is no geofence that it can monitor anymore, this needs to be fixed somehow.
            }

            //Retrieve GeofenceTransiton
            int geoFenceTransition = geofencingEvent.getGeofenceTransition();
            Log.d(TAG,"geofenceTransiton: "+geoFenceTransition);
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
        }
        //Error handeling

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


    /*
    *   GoogleApiClient connection
    * */

    private boolean createGoogleApi() {
        Log.d(TAG, "createGoogleApi()");
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            Log.d(TAG,"googleApiClient: "+googleApiClient.toString());
        }
        return true;
    }

/*
    private Runnable getGpsCoordinate = new Runnable() {
        @Override
        public void run() {

            count++;
            Log.i(TAG,"run time: "+count);

            //startInForeground();

            handler.postDelayed(this, 1000);// et sekund til neste itterasjon av tråd.
        }
    };
*/

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
                .setPriority(NotificationCompat.PRIORITY_MAX);//Makes the system prioritize this notification over the others(or the same as other with max

        //For å starte opp Loggin activity.
        Intent loggInIntent = new Intent(this,LoginAdminActivity.class);
        loggInIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingInlogging = PendingIntent.getActivity(this,0,loggInIntent,PendingIntent.FLAG_UPDATE_CURRENT);



        Intent stopKiosk = new Intent(STOP_KIOSK);
        PendingIntent pendingStopKiosk = PendingIntent.getBroadcast(getApplicationContext(),0,stopKiosk,PendingIntent.FLAG_UPDATE_CURRENT);

        //TODO: koble opp mot Innlogging, gjøres ved å ta ibruk

        //notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.addAction(R.drawable.login_48, "Log Inn", pendingInlogging );
        notificationBuilder.addAction(R.drawable.unlock_50, "Stop Kioks Mode",pendingStopKiosk);
        //PendingIntent resultPendingIntent = PendingIntent.getActivities(context, 0, logginIntent, 0);


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
/*
*   BroadcastReciever
*   Mottar Broadcast om ACTION_SCREEN_OFF.
*   Vekker enheten derskom dette mottas, slik at skjermen ikke skrus av.
* */
    public class ScreenOffReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"------------------------------------------------------------");
            Log.d(TAG,"VI mottok et intent:"+intent.getAction());
            switch (intent.getAction()){
                case Intent.ACTION_SCREEN_OFF:
                    Log.d(TAG, "*****************************************************");
                    Log.d(TAG, "Action Screen Off recieved");
                /*
                Intent i = new Intent(context, HomeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
                */
                    if(PreferenceUtils.isKioskModeActivated(context)){
                        getFullWakeLock().acquire();
                        getFullWakeLock().release();
                    }
                    break;
                case GeofenceTransitionService.STOP_KIOSK:
                    Log.d(TAG, "Vi setter Kioks mode til false");
                    Log.d(TAG,"Kiosk mode nå: "+PreferenceUtils.isKioskModeActivated(context));
                    PreferenceUtils.setKioskModeActive(context,false);
                    Log.d(TAG,"Kiosk mode nå etter forandring: "+PreferenceUtils.isKioskModeActivated(context));
                    break;
            }
           /* if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                Log.d(TAG, "*****************************************************");
                Log.d(TAG, "Action Screen Off recieved");

                Intent i = new Intent(context, HomeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);

                if(PreferenceUtils.isKioskModeActivated(context)){
                    getFullWakeLock().acquire();
                    getFullWakeLock().release();
                }
            }else if(intent.getAction().equalsIgnoreCase(STOP_KIOSK)){
                PreferenceUtils.setKioskModeActive(context,false); //Skrur av Kiosk mode.
            }
            */
        }
    }

    public void wakeUpDevice(){
        PowerManager powerManager = ((PowerManager) getSystemService(Context.POWER_SERVICE));
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.PARTIAL_WAKE_LOCK  ,"tag");
        wakeLock.acquire();
    }

    public PowerManager.WakeLock getWakeLock(){
        if(wakeLock == null){
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock( PowerManager.ACQUIRE_CAUSES_WAKEUP, "wakeup");
        }
        return wakeLock;
    }

    //SCREEN_BRIGHT_WAKE_LOCK

    public PowerManager.WakeLock getWakeLockNew(){
        if(wakeLock == null){
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK  , "wakeup");
        }
        return wakeLock;
    }

    PowerManager.WakeLock fullWakeLock;
    public PowerManager.WakeLock getFullWakeLock(){

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if(fullWakeLock == null){
                return fullWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "FULL WAKE LOCK");
            }
        return fullWakeLock;
    }

    /*
    *   End Keep screen awake code.
    * */


    @Override
    public Location getLastLocation(GoogleApiClient googleApiClient) {
        return null;
    }

    @Override
    public LocationAvailability getLocationAvailability(GoogleApiClient googleApiClient) {
        return null;
    }

    @Override
    public PendingResult<Status> requestLocationUpdates(GoogleApiClient googleApiClient, LocationRequest locationRequest, LocationListener locationListener) {
        return null;
    }

    @Override
    public PendingResult<Status> requestLocationUpdates(GoogleApiClient googleApiClient, LocationRequest locationRequest, LocationListener locationListener, Looper looper) {
        return null;
    }

    @Override
    public PendingResult<Status> requestLocationUpdates(GoogleApiClient googleApiClient, LocationRequest locationRequest, LocationCallback locationCallback, Looper looper) {
        return null;
    }

    @Override
    public PendingResult<Status> requestLocationUpdates(GoogleApiClient googleApiClient, LocationRequest locationRequest, PendingIntent pendingIntent) {
        return null;
    }

    @Override
    public PendingResult<Status> removeLocationUpdates(GoogleApiClient googleApiClient, LocationListener locationListener) {
        return null;
    }

    @Override
    public PendingResult<Status> removeLocationUpdates(GoogleApiClient googleApiClient, PendingIntent pendingIntent) {
        return null;
    }

    @Override
    public PendingResult<Status> removeLocationUpdates(GoogleApiClient googleApiClient, LocationCallback locationCallback) {
        return null;
    }

    @Override
    public PendingResult<Status> setMockMode(GoogleApiClient googleApiClient, boolean b) {
        return null;
    }

    @Override
    public PendingResult<Status> setMockLocation(GoogleApiClient googleApiClient, Location location) {
        return null;
    }

    @Override
    public PendingResult<Status> flushLocations(GoogleApiClient googleApiClient) {
        return null;
    }

}
