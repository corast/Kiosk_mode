package com.sondreweb.kiosk_mode_alpha.services;

/**
 * Created by sondre on 03-Mar-17.
 */

import android.app.AlarmManager;
import android.app.LauncherActivity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
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
import com.sondreweb.kiosk_mode_alpha.HudView;
import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.activities.LoginAdminActivity;
import com.sondreweb.kiosk_mode_alpha.classes.GeofenceStatus;
import com.sondreweb.kiosk_mode_alpha.storage.SQLiteHelper;
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

/**
*   THREAD HANDELING
*   Activity.runOnUiThread(Runnable)
*   View.post(Runnable)
*   View.postDelayed(Runnable, long)
* */


/**
 *  Hendelser som starter Servicen, som altså kjører OnStartCommand.
 *      1-Start fra Home screen.
 *
 *      2-Start fra RestartBroadCastReceiver
 *          a) Må sjekke om vi skal være i Kiosk_mode
 *              1: Da må vi gjøre alt på nytt egentlig, Lage Geofencene, Starte location request, og legge til Geofence i Monitorering.
 *      3-Start fra StartGeofenceKnapp, som signalisere at vi skal starte å tracke med geofencene.
 *
 *
 *        Geofence monitorering som er nødvendig:
 *          1. Lage en liste med alle geofence, dette kan forhåpentligvis gjøres ved hvert triggering event en gang.
 *
 *           Når Trigger intreffer må vi oppdatere denne listen.
 *              Listen skal inneholde GeofenceID + sisteEvent som intraff på de.
 *
 *
 * **/

    /*  TODO: Detect actions from system from broadcastReciever:
    *       android.intent.action.ACTION_SHUTDOWN
    *       android.intent.action.QUICKBOOT_POWEROFF
    *       BOOT_COMPLETED
     *        -> Starting av geofence når vi oppdater
    *
    * */

    /**
     * Steg som må gjennomføres for å starte opp GeofenceMonitorering.
     *  1. Request permission til å bruke Location
     *  2. Konfigurer Google Play Services.
     *      a) Configurer Library dependencies.
     *      b) Setup & Call googleApiClient( Connect til Google Api Clienten når den er lagd.)
     *  3. Start location monitorering.
     *      a) Lag LocationRequests.
     *
     *
     *
     *
     *  1. Lag GoogleApiClient.
     *  2. GoogleApiClient connect.
     *  3. Lag Geofencene vi trenger.
     *  4. Lag GeofenceRequest.
     *  5. sende dette GeofenceRequest  LocationServices.GeofencingApi.addGeofences(GeofenceRequest)
     *  6.
     *
     * **/

public class GeofenceTransitionService extends Service implements
        LocationListener, FusedLocationProviderApi, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status>{

    private static final String STOP_KIOSK = "com.sondreweb.STOP_KIOSK";

    private static boolean geofence_running = false;
    GoogleApiClient googleApiClient;


        //Testing:
        private Button overLayButton;

    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤Broadcast reviever TAGS¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤*/
    //Tags that we are listening for go here, to keep track of everyone this service is listening for.
    private static final String INTENT_FILTER_TAG = "com.sondreweb.GeoFencingAlpha.Activity.RestartGeofencing";

    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤END TAGS¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤*/

    public static final String OUTSIDE_GEOFENCE_TRIGGERED = "com.monumentvandring.launcher.outside.geofence";
    /*
    *   Når mobilen restartes av en eller ann grunn og viss den forsatt skal være i Kiosk mode, må vi reRegistrere Geofencne våre. Siden de forsvinner fra minne.
    * */
    public static final String START_GEOFENCE = "com.geofence.start";
    public static final String STOP_GEOFENCE_MONITORING = "com.geofence.stop.monitoring";
    public static final String RESTART_GEOFENCES = "com.geofence.restart";

    public static final String START_SERVICE = "com.geofence.service.start";

    public static final String TRIGGERED_GEOFENCE = "com.geofence.triggered";

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

    private ServiceBroadcastReceiver serviceBroadcastReceiver;

    private PowerManager.WakeLock wakeLock;
    private boolean isLocationPollingEnabled = true;
    private static Context sContext;

    private static final int vibrateTime = 500; //500 milisekunder.

    //Holder oversikt over alle statusene på Geofencene. Om vi er innefor ett eller flere geofence sammtidig.
    private List<GeofenceStatus> geofenceStatusList = null;

        @NonNull
        private String getGeofenceStatus() {
            if(geofence_running){
                return getResources().getString(R.string.service_geofence_on);
            }
            return getResources().getString(R.string.service_geofence_off);
        }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");
        //locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        notificationMananger = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(this); //Instansiate the NotificatioBuilder.
        startInForeground(); //setter opp notifikasjonen for å kjøre i forgrunn.

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(STOP_KIOSK);
        serviceBroadcastReceiver = new ServiceBroadcastReceiver();

        // Registrer Recievereren vi trenger for å fange SCREEN_OFF.
        registerReceiver(serviceBroadcastReceiver, filter);
        sContext = getApplicationContext();
    }

    public Context getsContext(){
        if(sContext == null){
            sContext = getApplicationContext(); //kan være null i noen tilfeller.
        }
        return sContext;
    }


    //Pending intents will start This again(i think).
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG,"onStartCommand: intentAction: "+ intent.getAction()+" ##################################################");

        // we can also check wheter the action is from the GeofenceClass or simply starting up the service again.
         //dersom vi starter servicen med hensikt å starte lokasjons håndtering

        GeofencingEvent geofencingEvent = null;
        //Switcher på hvilke Action vi har fått inn på intent.
        switch (intent.getAction()) {

            case START_GEOFENCE: //når vi trykker på knappen for å starte opp locationRequests og slike ting.
                Log.d(TAG, "Start LocationRequests fra servicen  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤");
                //TODO: opprette Geofencene og lagre hvor vi er innefor og slikt.
                if (createGoogleApi()) {
                    if (!googleApiClient.isConnected()) {
                        googleApiClient.connect(); //Connecter til googleApiClient.
                    } else { //Dersom man allerede er connectet så må man sjekke om vi må refreshe GeofenceListen vår.
                        //TODO: hent alle geofence vi har nå og hent en ny list fra database, dersom de er like så er det ikke noe å gjøre annet å sjekke at vi forsatt monitorere de.
                    }

                }
                break;
            case RESTART_GEOFENCES:
                //Dersom enheten er restertet under vandring.
                //TODO: reRegistrer Geofencene fra , og sørg for at vi forsatt fdatabasenår hentet ut Location en gang i blandt.
                if(createGoogleApi()){
                    if(!googleApiClient.isConnected()){
                        googleApiClient.connect();
                    }
                    //TODO gjør det vi må. her.
                }
                break;
            case TRIGGERED_GEOFENCE: //betyr at vi er kommet hit pga et eller flere Geofence er triggered.
                Log.d(TAG, "Triggered Geofence motatt ++++++++++++++");
                //TODO: gjør oppdatering på geofencene.
                geofencingEvent = GeofencingEvent.fromIntent(intent); //henter Geofencet fra intent viss intent kommer fra et GeofenceClass.
                if (geofencingEvent.hasError()) {
                    /*
                    *   Er 3 forskjellige Error meldinger:
                    *           GEOFENCE_NOT_AVAILABLE : Geofence service is not available now.
                    *           GEOFENCE_TOO_MANY_GEOFENCES : Your app has registered more than 100 geofences.
                    *           GEOFENCE_TOO_MANY_PENDING_INTENTS : You have provided more than 5 different PendingIntents to
                    *                               the addGeofences(GoogleApiClient, GeofencingRequest, PendingIntent) call.
                    * */
                    if( ! AppUtils.checkLocationAvailabillity(getsContext(),getGoogleApiClient())){
                        //Betyr at vi ikke har Location eller GoogleAPiClient ikke har Location tilgjengelig.
                        Log.e(TAG,"Vi har ikke location tilgjengelig");
                    }
                } else {
                //sender event videre.
                updateGeofenceStatus(geofencingEvent.getGeofenceTransition(), geofencingEvent.getTriggeringGeofences());
                }
                break;
            case START_SERVICE:
                break;
            case STOP_GEOFENCE_MONITORING:
                stopGeofenceMonitoring();
                break;
            default:
                //Dette vill si at vi servicen starter opp av seg selv, eller at vi simpelten starter den opp i bakgrunn.
                break;
        }

        //this can create Error the first time this service is started. We will have to figure out what to do.

                //ellers så er det bare at vi starter Servicen fra hvilket som helst annet sted.

        //Error handeling

        /*TODO sjekk hvilken av eventene fant sted, dersom det er ENTER, så vet vi at brukeren befinner seg innenfor området, og kan trygt fortsette(starte opp Monumentvandringen)
         dersom det er EXIT, så må vi sende en advarsem som varer i 5 minutter om at brukeren må gå tilbake innenfor specifisert område. Dersom EMTER evemten trer inn, stopper vi denne
         Tellingen. Men dersom ENTER ikke finner sted, så må vi låse enheten, slik at den ikke kan brukes til annet enn å finne veien tilbake.
        */ //TODO: Compass for navigasjon for brukeren, med en pil i retning mot sentrum av Geofencet. (Dette bør gå ganske greit).
        //startGeofencing();



        // TODO: start in foreground(to show notification that service is running to confirm).

        return START_STICKY; //When service is killed by the system, it will start up again.

    }
        /**
         *
         *  StartGeofenceMonitoring:
         *  lager nødvendige Lister.
         *  Henter Alle Geofence fra databasen og starter opp det med å monitorere osv.
         */

    public void startGeofenceMonitoring(){
        geofenceStatusList = new ArrayList<>();
        //Vi er conncted til GoogleApiClient

        //Mangler å lage Geofencene fra SQL databasen.

        List<Geofence> geofenceList = SQLiteHelper.getInstance(getsContext()).getAllGeofences();
        SQLiteHelper.getInstance(getsContext()).getAllGeofences();

        //Vi må lage en Liste med Geofence Statuser utifra dette.
        for (Geofence geofence:
             geofenceList) {
            //For hvert geofence, må vi lage en liten oversiktsliste.
            geofenceStatusList.add(new GeofenceStatus(geofence));
        }

        //sender med alle Geofence som vi skal monitorere for å lage et GeofenceRequest av disse.

        GeofencingRequest geofenceRequest = createGeofenceRequest(geofenceList);

        //Starter locationUpdates.
        startLocationUpdates(getsContext());

        //Sender dette GeofenceReuestet videre til LocationServices.GeofencingApi.addGeofences.
        addGeofencesToMonitor(geofenceRequest);

        setGeofence_running(true);
        //Vi starter ikke locationUpdates enda, men tror vi må det også.

    }

    /*
    *   Geofence har alle Request Id lik geofence_+(Tall de registret i listen)
    * */

    //Oppdatere de ulike geofencene våre

    //Må lage listen med Geofence oversikt.

    private void updateGeofenceStatus(int geofenceTransition,List<Geofence> triggeredeGeofences){

        /*
        if(AppUtils.DEBUG){
            Log.d(TAG,"før oppdatering:");
            for(GeofenceStatus geofenceStatus :geofenceStatusList){
                Log.d(TAG,"Geofence navn:"+ geofenceStatus.getGeofence().getRequestId() +", innefor status:"+geofenceStatus.getInsideStatus());
            }
        } */

        //For hvert Geofence som triggere må vi finne tilsvarende GeofenceStatus og oppdatere denne.
        for (Geofence geofence : triggeredeGeofences)
        {
            for(GeofenceStatus geofenceStatus : geofenceStatusList){
                if(geofence.getRequestId().equalsIgnoreCase(geofenceStatus.getGeofence().getRequestId())){
                    //siden de er like, så må vi oppdatere denne
                    geofenceStatus.setStatus(geofenceTransition);
                }
            }
        }

        if(AppUtils.DEBUG){
            Log.d(TAG,"Etter oppdatering:");
            for(GeofenceStatus geofenceStatus :geofenceStatusList){
                Log.d(TAG,"Geofence navn:"+ geofenceStatus.getGeofence().getRequestId() +", innefor status:"+geofenceStatus.getInsideStatus());
            }
        }
        //TODO sjekk om vi er innefor minst ett Geofence.

        if( ! checkIfInsideAtleastOneGeofence()){
           //TODO: Si ifra om at brukeren har beveget seg utenfor Geofencet vårt.
            if(AppUtils.DEBUG){
                Log.d(TAG,"VIKTIG: Vi er utenfor geofencene!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
            fastLocationUpdates = true; //setter slik at vi skal starte med fast LocationUpdates.
            //startFastLocationUpdates();

            //starte alarm som skal gå av inne 5 minutter og varsle brukeren på å bevege seg innenfor Geofence igjenn.
            AlarmManager alarmManager = (AlarmManager) getsContext().getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(getsContext(),GeofenceTransitionService.class);
            alarmIntent.setAction(GeofenceTransitionService.OUTSIDE_GEOFENCE_TRIGGERED);

            /*
            *   Lager et pendintIntent som sender en broadcast, med intent alarmIntent.
            *   Vi må lytte på denne i systemet som skal håndtere dette.
            *   context, privateRequestCode, intent, flagg
            * */
            PendingIntent alarmPintent = PendingIntent.getBroadcast(getsContext(),0, alarmIntent,0);

            vibratePhone(500);
            //kan virbrere telefonen her, men er bedre om aktivitetn i vindu gjør dette for oss.



            Handler mHandler = new Handler();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getsContext(), "Vi er utenfor et geofence!!", Toast.LENGTH_SHORT).show();
                }
            });

            //TESTING av å vise noe på skjermen.
            //tellUserToGoInsideGeofence();
            tellUserToGoInsideButton();

            /*
            Log.d(TAG, "lager en alarm som går av etter :" + (10)+" sekunder");
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+
                    10*1000, alarmPintent);
                    */
        }else{
            //Går tilbake til trengere intervall.
            fastLocationUpdates = false;
            if(AppUtils.DEBUG){
                Log.d(TAG,"Vi er innefor minst ett geofence");
                WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                try {
                    if(overLayButton != null){
                        windowManager.removeView(overLayButton);
                    }
                }catch (IllegalArgumentException e){
                    Log.e(TAG, e.getMessage());
                }
            }
            startLocationUpdates(getsContext());
        }
    }

    HudView mView;

    public void tellUserToGoInsideGeofence(){

        mView = new HudView(this);
        //HudView mView = new HudView(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();

        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

        //params.gravity = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        params.format = PixelFormat.TRANSLUCENT; //gjennomsiktig view.
        params.gravity = Gravity.RIGHT | Gravity.TOP;
        params.setTitle("Load Average");
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(mView, params);


    }

    public LinearLayout linearLayout;

    public void tellUserToGoInsideLinearLayout(){

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View screenView = inflater.inflate(R.layout.activity_home,null);

        linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setBackgroundResource(R.color.lightGreen);

        TextView textView = new TextView(this);
        textView.setText(getResources().getString(R.string.service_geofence_outside_view_text));
        textView.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        textView.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

        linearLayout.addView(textView);
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(linearLayout);

    }

    public void tellUserToGoInsideButton(){
        overLayButton = new Button(this);
        overLayButton.setText("Tekst");
        overLayButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        overLayButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //Toast.makeText(getApplicationContext(), "Testing", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        overLayButton.setBackgroundResource(R.color.lightGreen);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(overLayButton, params);

    }

        //Sjekker listen på om vi er innefor minst ett geofence.
    public boolean checkIfInsideAtleastOneGeofence(){
        //Må sjekke om alle er false.
        //altså vi må sjekke om minst et er true, så kan vi returne.
        for (GeofenceStatus geofenceStatus: geofenceStatusList) {
            //geofenceStatus returnere True dersom siste trigger er ENTER på geofencet.
            if(geofenceStatus.getInsideStatus()){
                return true;
            }
        }
        return false;
    }


    //For når brukere bevegers seg utenfor Geofence og vi trenger å få deres oppmerksomhet.
    public void vibratePhone(int timeInMillis){
        //vibre i angitte time i ms.
        Vibrator vibrator = (Vibrator) this.getsContext().getSystemService(Context.VIBRATOR_SERVICE);

        vibrator.vibrate(timeInMillis); //Hvor lenge skal vi vibrere?

    }

/*
*   Geofence funksjoner nedover her...
* */

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

    /* GET FUNCTIONS */
    public GoogleApiClient getGoogleApiClient(){
        if(googleApiClient != null){
            return googleApiClient;
        }
        this.createGoogleApi();
        return googleApiClient;
    }

    /*  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *   GeofenceRequest START
    *
    *   GeofenceBuilder.setInitialTrigger: Sets the geofence notification behavior at the moment when the geofences are added.
    *   Vi setter GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_EXIT , som betyr:
    *   Alle vi er innenfor triggere Enter, mens alle vi er utenfor triggere EXIT. VI får da 2 EventLister tilbake med geofence tilbake.
    *   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */

    //create GeofenceRequests, vi spesifisere også hva som triggeres

    @NonNull
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        Log.d(TAG, "createGeofenceRequest()");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_EXIT)  //.setInitialTrigger: Sets the geofence notification behavior at the moment when the geofences are added.
                .addGeofence(geofence) //.addGeofence : Adds a geofence to be monitored by geofencing service.
                .build();
    }  //Kan hente alle geofence med denne Requesteren.

    //GeofenceBuilder.setInitialTrigger: Sets the geofence notification behavior at the moment when the geofences are added.
    /*
    *   Legger til flere Gofence samtidig i en Request.
    *
    * */
    @NonNull
    private GeofencingRequest createGeofenceRequest(List<Geofence> geofences) {
        Log.d(TAG, "createGeofenceRequest()");
        return new GeofencingRequest.Builder()
                //.setInitialTrigger: Sets the geofence notification behavior at the moment when the geofences are added.
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_EXIT)
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

    private void addGeofencesToMonitor(GeofencingRequest geofenceRequest) {
        if(AppUtils.DEBUG){
            Log.d(TAG, "addGeofence()");
        }
        if (AppUtils.checkLocationPermission(getsContext())) {//om vi har rettigheter til å få tilgang til Lokasjon.
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient, //GoogleApiClienten servicen har koblet seg til.
                    geofenceRequest, //GeofenceRequestet vi lagde med alle geofencene våre.
                    getGeofencePendingIntent() //Henter PendingIntent, eller lager dersom det er tomt.
            ).setResultCallback(this); //ResultCallback -> OnResult.
        } else{
            if(AppUtils.DEBUG){
                Log.d(TAG, "addGeofence: Manglet permission");
            }
        }
    }


    /*
    *   Stop monitorering av Geofencene, siden vi ikke skal ha enheten i bruk lenger.
    *   Skal aktivers når vi tryker på å skru av Kiosk mode.
    * */

    public void stopGeofenceMonitoring(){
        if(googleApiClient != null) {
            setGeofence_running(false); //Setter denne til false;
            LocationServices.GeofencingApi.removeGeofences(
                    googleApiClient,
                    getGeofencePendingIntent()
            ).setResultCallback(this);

            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            if (googleApiClient.isConnected()) {
                getGoogleApiClient().disconnect();
            }
        }
    }

     /*
     * In case network location provider is disabled by the user, the geofence service will stop updating, all registered geofences
     * will be removed and an intent is generated by the provided pending intent.
     * In this case, the GeofencingEvent created from this intent represents an error event,
     * here hasError() returns true and getErrorCode() returns GEOFENCE_NOT_AVAILABLE.
     * */
    private int GEOFENCE_REQ_CODE = 0;

    private PendingIntent geoFencePendingIntent;

            /* Intent for servicen som skal håndtere Geofence eventene */
    private PendingIntent getGeofencePendingIntent() {
        Log.d(TAG, "getGeofencePendingIntent()");
        //gjenbruker Pendingintentet vårt dersom der alt er laget.
        if (geoFencePendingIntent != null) {
            return geoFencePendingIntent;
        }

        Intent intent = new Intent(this, GeofenceTransitionService.class); //The intent wich will be called when events trigger.
        intent.setAction(TRIGGERED_GEOFENCE); //Legger til en string, slik at vi kan finne ut om det inneholder et Geofence event eller ikke.
        //FLAG_UPDATE_CURRENT slik at vi får samme Pending intent tilbake når vi kaller addGeofence eller Remove Geofence.
        geoFencePendingIntent = PendingIntent.getService(this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geoFencePendingIntent;

        //PendingIntent.getService returnerer PendingIntent som skal starte servicen
        /*Retrieve a PendingIntent that will start a service, like calling Context.startService(). The start arguments given to the service will come from the extras of the Intent.
        * */
    }



    /* Notification build up:
    * | Icon | Title
    *         SubText
    * */

    private void startInForeground(){ //vi må kalle denn viss vi skal oppdatere notifikasjonen.
        Log.d(TAG,"startInForeground() / Update notification");
        //TODO: use input to change the notification.

        //TODO: Figure out what pending intent does here.

        notificationBuilder
                .setSmallIcon(getNotificationIcon()) //icon that the user see in status bar.
                .setContentTitle(getResources().getString(R.string.service_title)) // GeofenceClass Service
                .setContentText(getResources().getString(R.string.service_text)) // Text; Location being monitored.
                .setSubText(getResources().getString(R.string.service_status_geofence) + " " + getGeofenceStatus()) //lan lot centrum geofence
                .setTicker("Service starting") //geofence running
                .setPriority(NotificationCompat.PRIORITY_MAX);//Makes the system prioritize this notification over the others(or the same as other with max

        //For å starte opp Loggin activity.

        Intent loggInIntent = new Intent(this,LoginAdminActivity.class);
        loggInIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingInlogging = PendingIntent.getActivity(this,0,loggInIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        //tømmer alle actionene på notifikasjonsbare, siden vi adder flere senere.
        notificationBuilder.mActions.clear();

        //Intent for å sette KioskMode til OFF/kunn for testing.
        if(AppUtils.DEBUG){
            Intent stopKiosk = new Intent(STOP_KIOSK);
            PendingIntent pendingStopKiosk = PendingIntent.getBroadcast(getsContext(),0,stopKiosk,PendingIntent.FLAG_UPDATE_CURRENT);
            //notificationBuilder.addAction(R.drawable.unlock_50, "Stop Kioks Mode",pendingStopKiosk);
        }
        //TODO: koble opp mot Innlogging, gjøres ved å ta ibruk
        //notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.addAction(R.drawable.login_48, "Log Inn", pendingInlogging );

        //PendingIntent resultPendingIntent = PendingIntent.getActivities(context, 0, logginIntent, 0);

        startForeground(mId, notificationBuilder.build()); //Start showing the notification on the (Action)/task bar.
    }

    private int getNotificationIcon(){//Versjons kontroll på hvilket bilde å bruke.
        boolean useWhiteIcon = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
        //return useWhiteIcon ? R.drawable.visible_64 : R.drawable.visible_50;
        return R.drawable.visible_50;
        
        // FIXME: 10-Apr-17
    }

    private void setGeofence_running(boolean running){
        geofence_running = running;
        startInForeground();//oppdatere notifikasjonen vår.
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        Intent broadcastIntent = new Intent(INTENT_FILTER_TAG);
        stopForeground(true);
        //handler.removeCallbacks(getGpsCoordinate); //fjerner tråden.
        //handler.notify(); //bare tester hva denne gjør.
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
    public class ServiceBroadcastReceiver extends BroadcastReceiver {

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
                    if(PreferenceUtils.isKioskModeActivated(context)){ //vi vekker bare skjermen dersom dette.
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
        }
    }

    //###############################LOCATION UPDATE STARTING############################


    private LocationRequest locationRequest;
    private LocationRequest fastLocationRequest;
    private boolean fastLocationUpdates = false;


    /*
    *   setPriority: PRIORITY_BALANCED_POWER_ACCURACY : request "block" level accuracy.
    *                PRIORITY_HIGH_ACCURACY : request the most accurate locations available.
    *                PRIORITY_LOW_POWER : request "city" level accuracy.
                     PRIORITY_NO_POWER : request the best accuracy possible with zero additional power consumption.
    * */

    //starter location updates.
    public void startLocationUpdates(Context context) {//TODO: Bytt til GPS kordinater, fremfor Wifi etc.
        Log.i(TAG, "startLocationUpdates()");
        //henter intervall fra SharedPreferences, default så er interval på tiden 2 minutter, og fastestIntervall er på 10 sekunder.
        /*
        * setIntervall vill si tiden mellom hver etterspørsel av lokasjon. Ivertfall da vi prøver å hente lokasjon.
        *   dersom dette ikke skulle gå så får vi ikke noen lokasjon til neste intervalls tid.
        * setFasterIntervall forteller hvor ofte vi tillater oss å lese av nyeste lokasjon og oppdatere vår egen,
        *     dersom andre applikasjoner osv henter lokasjon sammtidig, kan vi også få inn resultatet.
        *

        * */

        //Må sjekke om fastLocationRequest er null og at boolean på at vi er utenfor er satt.
        if (fastLocationUpdates) {
            //siden denne er true, så må vi anta at vi forsatt driver med gamle Reuquesten med googleApiClient.
        } else {
        locationRequest = locationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) //request the most accurate locations available. (tror dette alltid er GPS).
                .setInterval(PreferenceUtils.getPrefGeofenceUpdateInterval(context))
                .setFastestInterval(PreferenceUtils.getPrefGeofenceFastestUpdateInterval(context));

        if (AppUtils.checkLocationPermission(context)) {
            //starter å hente lokasjonen med requesten.
            Log.d(TAG, "startLocationUpdate() checkLocationPermission: true");
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            isLocationPollingEnabled = true;
            //this refererer til callbacks, som er oss selv.
        }
        //locationRequest.
        }
    }

    public void setLocationUpdateChange(boolean fastUpdate){
        fastLocationUpdates = fastUpdate;
        if(fastLocationUpdates){
            startLocationUpdates(getsContext());
        }else{
            startFastLocationUpdates();
        }
    }


        private void startFastLocationUpdates(){
            LocationServices.FusedLocationApi.removeLocationUpdates(getGoogleApiClient(), this);

            fastLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(PreferenceUtils.THIRTY_SECONDS_IN_MILLIS)
                    .setFastestInterval(PreferenceUtils.ONE_SECOND_IN_MILLIS);

            if (AppUtils.checkLocationPermission(getsContext())) {
                //starter å hente lokasjonen med requesten.
                if(AppUtils.DEBUG) {
                    Log.d(TAG, "startLocationUpdate() checkLocationPermission: true");
                }

                LocationServices.FusedLocationApi.requestLocationUpdates(getGoogleApiClient(),fastLocationRequest,this);

                isLocationPollingEnabled = true;
            }
        }



    //###############################LOCATION UPDATE ENDING############################
    /*
    *   Wake screen kode.
    * */

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

    /*  ResultCallback
    *
    *   Ved addGeofencesToMonitor får vi tilbake melding her på hvordan dette gikk
    *
    * */

    @Override
    public void onResult(@NonNull Status status) {
        Log.i(TAG,"onResult(): "+status);
        if(status.isSuccess()){
            //Toast.makeText(this,"Geofences created successfully", Toast.LENGTH_SHORT).show();
        }else if(status.hasResolution()){
           //TODO: feilmeldinger på å lage Geofence.
            Log.d(TAG,"statuscode: "+status.getStatusCode()+"\n statusMessage: "+status.getStatusMessage()+" \nstatusResolution: "+status.getResolution());
        }
        else
        {
            Log.e(TAG, "Registering failed: " + status.getStatusMessage()+ " code:"+status.getStatusCode());
        }
    }

    /*
    *   LocationRequest
    * */

    /**¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
     *  LocationListener
     *  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
     */

    @Override
    public void onLocationChanged(Location location) { //lytter til når location forandres, på denne måten kan vi lytte etter locationChange flere steder.
        //TODO: gjør noe med denne eller?
        if(AppUtils.DEBUG) {
            Log.d(TAG, "LocationChanged location: " + location.getLatitude() +", "+ location.getLongitude());
        }

    }
    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

    /*   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *           GoogleApiClient.ConnectionCallbacks
    *    ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */

    @Override
    public void onConnectionSuspended(int cause) {
        if(PreferenceUtils.isKioskModeActivated(getsContext())){
            //Dersom vi forsatt skal egentlig være connected, så er dette et problem.
            if(AppUtils.DEBUG){
                Log.d(TAG,"onConnectionSuspended");
            }

            if(PreferenceUtils.isKioskModeActivated(getsContext())){
                //Dette kan våre et problem.
            }

        }

        /*
        * Called when the client is temporarily in a disconnected state.
        * This can happen if there is a problem with the remote service (e.g. a crash or resource problem causes it to be killed by the system).
         * When called, all requests have been canceled and no outstanding listeners will be executed. GoogleApiClient will automatically attempt to restore the connection.
        * Applications should disable UI components that require the service, and wait for a call to onConnected(Bundle) to re-enable them.
        * */
    }


    /*
    *   Når Vi connecter til Google API Client.
    *   Veldig viktig at vi her starter med allt vi må.
    * */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Dette betyr at vi kan lage geofencene våre og starte med alt som har med det å gjøre.
        if(AppUtils.DEBUG){
            Log.d(TAG, "onConnected()+++++++++++++++++++++++++++++++++++++++++++++++");
        }

        startGeofenceMonitoring();
        Log.d(TAG, "Vi er connected til google api client.");
    }
    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    /*  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *           GoogleApiClient.OnConnectionFailedListener
    *   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //TODO: be activity som motar meldingen starte
        //connectionResult.startResolutionForResult();
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
