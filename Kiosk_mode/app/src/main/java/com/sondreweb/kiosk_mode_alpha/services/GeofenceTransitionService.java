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
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
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
 *  Run events when the users reacts with a GeofenceClass.
 *  Lock the device if the user exits the Geofences.
 *  Must be efficient enught, ie not draw all the battery power afther an hour etc.
 *
 *  Must be able to start itself up again when the system reboots, starts.
 *  Tasked with keeping the system only usable within the parameters(geofence).
 *  Tasked with Warning the user if he is almost exiting an GeofenceClass. (need different GeofenceClass levels).
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

    private final int UPDATE_INTERVAL = 20*1000;//20 seconds
    private final int FASTEST_INTERVAL = 4*1000;// 4 seconds.

    GoogleApiClient googleApiClient;

    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤Broadcast reviever TAGS¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤*/
    //Tags that we are listening for go here, to keep track of everyone this service is listening for.
    private static final String INTENT_FILTER_TAG = "com.sondreweb.GeoFencingAlpha.Activity.RestartGeofencing";

    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤END TAGS¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤*/


    public static final String START_GEOFENCE = "com.geofence.start";

    /*
    *   Når mobilen restartes av en eller ann grunn og viss den forsatt skal være i Kiosk mode, må vi reRegistrere Geofencne våre. Siden de forsvinner fra minne.
    * */
    public static final String RESTART_GEOFENCES = "com.geofence.restart";


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

    private static Context sContext;


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
        sContext = getApplicationContext();
    }

    public Context getsContext(){
        if(sContext == null){
            sContext = getApplicationContext(); //kan være null i noen tilfeller.
        }
        return sContext;
    }

    public void InitVars(){

    }

    //Pending intents will start This again(i think).
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG,"onStartCommand(intent, flag,startId) ##################################################");

        InitVars();

        // we can also check wheter the action is from the GeofenceClass or simply starting up the service again.
        if(intent.getAction().equalsIgnoreCase(START_GEOFENCE)){ //dersom vi starter servicen med hensikt å starte lokasjons håndtering
            Log.d(TAG,"Start LocationRequests fra servicen  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤");
            if(createGoogleApi()){

            }
        }

        switch (intent.getAction()){
            case START_GEOFENCE: //når vi trykker på knappen for å starte opp locationRequests og slike ting.
                Log.d(TAG,"Start LocationRequests fra servicen  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤");
                //TODO: opprette Geofencene og lagre hvor vi er innefor og slikt.
                break;
            case RESTART_GEOFENCES:
                //TODO: reRegistrer Geofencene fra databasen, og sørg for at vi forsatt får hentet ut Location en gang i blandt.
                break;

            default:
                //Dette vill si at vi servicen starter opp av seg selv, eller at vi simpelten starter den opp i bakgrunn.
                break;
        }

        //this can create Error the first time this service is started. We will have to figure out what to do.
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent); //henter Geofencet fra intent viss intent kommer fra et GeofenceClass.
                //ellers så er det bare at vi starter Servicen fra hvilket som helst annet sted.
        if(geofencingEvent != null ){
            Log.d(TAG,"GeofenceEvent: "+geofencingEvent.toString());
            if( geofencingEvent.hasError() ){ //TODO:finne ut hvorfor det har oppstått en error, og fikse dette
                String errorMessage = getErrorString( geofencingEvent.getErrorCode() );
                //dersom geofenceEvent.getErrorCode() returnere GEOFENCE_NOT_AVAILABLE, betyr dette at brukeren har disabled network location provider.
                //Dermed vill alle registrerte GeofenceClass bli fjernet og
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
*   Vi må lage en oversikt over de ulike Geofence og derese IDer, samt oppdatere denne oversikten hver gang et Geofence Event framkommer.
* */

    public static void getGeofences(){ //denne skal lagre alle IDer til geofencec, og lage en oversikt over hvilken vi er innefor.

    }

    /*
    *   Tar et geofence og oppdaterer statusen basert på denne
    * */
    public void updateGeofenceStatus(GeofencingEvent geofencingEvent){
        if(geofencingEvent == null){//antar det ikke er null og faktisk er et godkjent geofenceEvent.
            Log.e(TAG,"Error GeofenceEvent er NULL");
        }else if(geofencingEvent.hasError()){
            //Dersom det har en error, må vi finne ut hva denne er
            Log.e(TAG,"############Geofence Error######## :"+geofencingEvent.getErrorCode());
            /*
            *   Er 3 forskjellige Statusmeldinger:
            *           GEOFENCE_NOT_AVAILABLE : Geofence service is not available now.
            *           GEOFENCE_TOO_MANY_GEOFENCES : Your app has registered more than 100 geofences.
            *           GEOFENCE_TOO_MANY_PENDING_INTENTS : You have provided more than 5 different PendingIntents to
            *                               the addGeofences(GoogleApiClient, GeofencingRequest, PendingIntent) call.
            * */

        }else //viss vi har kommet hit så er alt OK.
        {
            geofencingEvent.getGeofenceTransition();
            //Må først sjekke hva som har skjedd
            switch (geofencingEvent.getGeofenceTransition()){

                case Geofence.GEOFENCE_TRANSITION_ENTER:

                    break;

                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    break;

            }
            //Henter lokasjon, dersom vi bruker Google Play services 5.0 SDK eller nyere.
            geofencingEvent.getTriggeringLocation();

        }
    }

    public void testing(){
        Geofence geofence = createGeofence(new LatLng(0.1412421,12412412),1231.14f);
        geofence.getRequestId();
    }

    private Geofence createGeofence(LatLng latLng, float radius){
        return new Geofence.Builder()
                .setCircularRegion(latLng.latitude, latLng.longitude,radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
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

    /**
     *  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
     *      Create GeofenceList
     *  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
     * **/

    private List<Geofence> createGeofences(){

        long expire = Geofence.NEVER_EXPIRE;
        int event = Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT;
        //setRequestID = Database.ID.

        ArrayList<Geofence> geofenceList = new ArrayList<Geofence>();

        //TODO: loop igjennom Databasen og hent alle geofencer, og gjør disse om til geofence objecter.

        return geofenceList;
    }

    /***
     *  End GeofenceClass
     * */

    /*  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *   GeofenceRequest START
    *   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */

    //create GeofenceRequests, vi spesifisere også hva som triggeres
    //GeofenceBuilder.setInitialTrigger: Sets the geofence notification behavior at the moment when the geofences are added.
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        Log.d(TAG, "createGeofenceRequest()");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)  //.setInitialTrigger: Sets the geofence notification behavior at the moment when the geofences are added.
                .addGeofence(geofence) //.addGeofence : Adds a geofence to be monitored by geofencing service.
                .build();
    }

    //GeofenceBuilder.setInitialTrigger: Sets the geofence notification behavior at the moment when the geofences are added.
    /*
    *   Legger til flere Gofence samtidig i en Request.
    *
    * */
    private GeofencingRequest createGeofenceRequest(List<Geofence> geofences) {
        Log.d(TAG, "createGeofenceRequest()");
        return new GeofencingRequest.Builder()
                //.setInitialTrigger: Sets the geofence notification behavior at the moment when the geofences are added.
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                //.addGeofences: Adds all the geofences in the given list to be monitored by geofencing service.
                .addGeofences(geofences)
                //.build: Builds the GeofencingRequest object.
                .build();
    }

    /*
    *   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *   GeofenceRequest END
    *   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */


    /*
    *   Legg til GeofenceRequest til device monitoring list.
    *
    * */
/*
    private void addGeofence(GeofencingRequest geofenceRequest) {
        Log.d(TAG, "addGeofence()");
        if (AppUtils.checkPermission(getsContext())) {//om vi har rettigheter til å få tilgang til Lokasjon.
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    geofenceRequest,
                    createGeofencePendingIntent()
            ).setResultCallback(this);
        } else
            Log.d(TAG, "addGeofence: Manglet permission");
    } */

        /*
        In case network location provider is disabled by the user, the geofence service will stop updating, all registered geofences
        will be removed and an intent is generated by the provided pending intent.
        In this case, the GeofencingEvent created from this intent represents an error event, where hasError() returns true and getErrorCode() returns GEOFENCE_NOT_AVAILABLE.
        * */

    private int GEOFENCE_REQ_CODE = 0;
    private PendingIntent geoFencePendingIntent;

            /* Intent for servicen som skal håndtere Geofence eventene */
    private PendingIntent createGeofencePendingIntent() {
        Log.d(TAG, "createGeofencePendingIntent()");
        //gjenbruker Pendingintentet vårt dersom der alt er laget.
        if (geoFencePendingIntent != null) {
            return geoFencePendingIntent;
        }

        Intent intent = new Intent(this, GeofenceTransitionService.class); //The intent wich will be called when events trigger.

        //FLAG_UPDATE_CURRENT slik at vi får samme Pending intent tilbake når vi kaller addGeofence eller Remove Geofence.
        return PendingIntent.getService(this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //PendingIntent.getService returnerer PendingIntent som skal starte servicen
        /*Retrieve a PendingIntent that will start a service, like calling Context.startService(). The start arguments given to the service will come from the extras of the Intent.
        * */
    }


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
                .setContentTitle("Title") // GeofenceClass Service
                .setContentText(msg) // Text; Location being monitored.
                .setSubText("SubText") //lan lot centrum geofence
                .setTicker("Service starting") //geofence running
                .setPriority(NotificationCompat.PRIORITY_MAX);//Makes the system prioritize this notification over the others(or the same as other with max

        //For å starte opp Loggin activity.
        Intent loggInIntent = new Intent(this,LoginAdminActivity.class);
        loggInIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingInlogging = PendingIntent.getActivity(this,0,loggInIntent,PendingIntent.FLAG_UPDATE_CURRENT);


        //Intent for å sette KioskMode til OFF/kunn for testing.
        Intent stopKiosk = new Intent(STOP_KIOSK);
        PendingIntent pendingStopKiosk = PendingIntent.getBroadcast(getsContext(),0,stopKiosk,PendingIntent.FLAG_UPDATE_CURRENT);

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
                return "GeofenceClass not available";
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

    //###############################LOCATION UPDATE STARTING############################

    private LocationRequest locationRequest;

    /*
    *   setPriority: PRIORITY_BALANCED_POWER_ACCURACY : request "block" level accuracy.
    *                PRIORITY_HIGH_ACCURACY : request the most accurate locations available.
    *                PRIORITY_LOW_POWER : request "city" level accuracy.
                     PRIORITY_NO_POWER : request the best accuracy possible with zero additional power consumption.
    * */



    //starter location updates.
    public void startLocationUpdates(Context context) {//TODO: Bytt til GPS kordinater, fremfor Wifi etc.
        Log.i(TAG, "startLocationUpdates()");
        locationRequest = locationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) //request the most accurate locations available. (tror dette alltid er GPS).
                .setInterval(PreferenceUtils.getPrefGeofenceUpdateInterval(context))
                .setFastestInterval(PreferenceUtils.getPrefGeofenceFastestUpdateInterval(context));

        if (AppUtils.checkPermission(context)) {
            //starter å hente lokasjonen med requesten.
            Log.d(TAG, "startLocationUpdate() checkPermission: true");
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
        //locationRequest.
    }

/*
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
    */

    PowerManager.WakeLock fullWakeLock; //Holder på Wakelocken. Viss vi trenger den til senere.

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


    /*
    *   LocationRequest
    * */

    /**¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
     *  LocationListener
     *  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
     */

    @Override
    public void onLocationChanged(Location location) { //lytter til når location forandres, på denne måten kan vi lytte etter locationChange flere steder.

    }
    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

    /*   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *           GoogleApiClient.ConnectionCallbacks
    *    ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }
    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    /*  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *           GoogleApiClient.OnConnectionFailedListener
    *   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *         FusedLocationProviderApi
    * ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
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

    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *         FusedLocationProviderApi END
    * ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */

}
