package com.sondreweb.kiosk_mode_alpha.activities;

import android.app.ActivityManager;
import android.app.UiModeManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.sondreweb.kiosk_mode_alpha.CustomView;
import com.sondreweb.kiosk_mode_alpha.GridItemListener;
import com.sondreweb.kiosk_mode_alpha.StatusInfo;
import com.sondreweb.kiosk_mode_alpha.adapters.StatusAdapter;
import com.sondreweb.kiosk_mode_alpha.deviceAdministator.DeviceAdminKiosk;
import com.sondreweb.kiosk_mode_alpha.services.GeofenceTransitionService;
import com.sondreweb.kiosk_mode_alpha.utils.AppUtils;
import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;
import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.services.AccessibilityService;

import java.util.ArrayList;

/**
 * Created by sondre on 16-Feb-17.
 *
 * Home screen appen, denne har ansvar for å vise  tilgjengelige applikasjoner.
 * TODO: Lag en knapp som starter Kiosk mode(altså starter opp monumentvandringen, vi har ikke lov å gå ut av denne.
 * TODO: Ta i bruk SettingsApi, ved å først koble til Google Api Client.
 */
public class HomeActivity extends FragmentActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    public final static String TAG = HomeActivity.class.getSimpleName();
    private final static String APP = "com.sondreweb.geofencingalpha";

    private TextView statusText, googleClientText;

    private GridView gridView;

    private Button startKioskButton;

    private View decorView;
    private PackageManager packageManager;
    private Context context;

    private DevicePolicyManager devicePolicyManager;

    //vi holder på googleApiClienten vår her.
    private static GoogleApiClient googleApiClient;

    Intent intent = new Intent(Intent.ACTION_VIEW);

    static { //for passord ting.
        //System.loadLibrary("");
        Log.d(TAG,"Static tag kjører........................................................................................................");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        //this.startActivity(intent);

        gridView = (GridView)findViewById(R.id.grid_status_view);
        gridView.setGravity(Gravity.FILL_HORIZONTAL);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG,"onItemClick listerner er trykket" + view.toString());
            }
        });
        startKioskButton = (Button) findViewById(R.id.button_start_kiosk);

        googleClientText = (TextView) findViewById(R.id.text_googleApiConnection);
        statusText = (TextView) findViewById(R.id.home_text);
        //Lager en ny Component Indentifier fra en classe. Men hvorfor?
            //TODO: finn ut hva dette faktisk gjør.
        ComponentName deviceAdmin = new ComponentName(this, DeviceAdminKiosk.class);

        //Henter DevicePolicMangar, brukes for å sjekke om vi er admin osl.
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        if(devicePolicyManager.isAdminActive(deviceAdmin)){
            PreferenceUtils.setPrefAdminDevice(true, this); //dersom vi er admin, så kan vi sette at vi faktisk er det i instillingene:
        }
        else
        {
            PreferenceUtils.setPrefAdminDevice(false,this); //dersom vi ikk er admin, så lagrer vi dette til senere bruk.
            Log.d(TAG,"vi er ikke admin");
            statusText.setText("Vi er ikke admin");
        }

        /* Initalize google API client.(Trenger ikke nyeste Versjon av Servicen for dette)
        * */
        createGoogleApi();

        /*if(AppUtils.isGooglePlayServicesAvailableAndPoll(this,this)){
            //createGoogleApi(); //lager GoogleApiClienten vår.
            googleApiClient.connect();
        } */

        decorView = getWindow().getDecorView();
        context = this;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        startAccessibilityService();
        //isAppInstalled(this, "testing");
        startConsumeView();
    }

    //Create GoogleApiClient Instance
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

    public static GoogleApiClient createGoogleApiClient(){
        if(googleApiClient != null){
            return googleApiClient;
        }else
          return null;
    }


    /*
    *   Liste som holder på alle statusene våre. Denne må oppdateres kontinuelig, ved forandring på systemet.
    * */
    public ArrayList<StatusInfo> statusList = new ArrayList<>() ;


    /*
    *   Lager en Liste med Statuser, som inneholder navn og en bool
    *   hvor boolen forteller om statusen er klar.
    *   Når alt er klart kan vi begynne Vandringen(MonumentVandring).
    * */
    public void createAndUpdateStatusList(){
        if(! statusList.isEmpty()){
            statusList = new ArrayList<>(); //tømmer listen.
        }
        //Må først sjekke
            StatusInfo status; //status objekt som vi bare kan bruke, mulig dette er litt dårlig, siden vi får samme objekt med ulike parametere.

        status = new StatusInfo("Google Play Services");
        if(AppUtils.isGooglePlayServicesAvailable(this)){
            status.setStatus(true);
        }else{
            status.setStatus(false);
        }
        statusList.add(status);

        status = new StatusInfo("Device Admin");
        if(PreferenceUtils.isAppDeviceAdmin(this)){
            status.setStatus(true);
        }else{
            status.setStatus(false);
        }
        statusList.add(status);

        status = new StatusInfo("Accessibility Service");
        if(AppUtils.isAccessibilitySettingsOn(this)){
            status.setStatus(true);
        }else{
            status.setStatus(false);
        }
        statusList.add(status);

        //sjekker touchVievet
        status = new StatusInfo("TouchView");
        if(isTouchViewVisible()){
            status.setStatus(true);
        }else{
            status.setStatus(false);
        }
        statusList.add(status);

        //

        Log.d(TAG, statusList.toString());


        StatusAdapter statusAdapter = new StatusAdapter(HomeActivity.this);
        statusAdapter.setData(statusList);
        gridView.setAdapter(statusAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG,view.toString());
            }
        });
    }

    public boolean allStatusTrue(){
        for(StatusInfo s : statusList){
            if(! s.getStatus()){ //dersom det failer en gang, så er det ikke klart til å starte.
                return false;
            }
        }
        return true;
    }

    public boolean allStatusTrueTest(){
       if(PreferenceUtils.isKioskModeActivated(context) && AppUtils.isAccessibilitySettingsOn(context)){
           return true;
       }
        return false;
    }


    @Override
    protected void onStart() {//Ved onstart burde vi sjekke ulike ting.
        Log.d(TAG,"onStart()");
        createGoogleApi(); //Tilfelle det er null.

        createAndUpdateStatusList(); //oppdaterer listen.

        if(PreferenceUtils.isAppDeviceAdmin(this)){
            statusText.setText("Vi er device admin");
        }else{
            statusText.setText("Vi er IKKE device admin");
        }

        if(AppUtils.isDeviceOwner(this)){
            statusText.append(" | Vi er Device Owner");
        }else{
            statusText.append(" | Vi er IKKE Device Owner");
        }

        if(AppUtils.isAccessibilitySettingsOn(this)){
            statusText.append(" | Accessibillity service er på");
        }else{
            statusText.append(" | Accessibility service er IKKE på");
        }

        if(AppUtils.isServiceRunning(GeofenceTransitionService.class,this)){
            statusText.append("\nGeofenceTransitionService kjører");
        }else{
            statusText.append("\nGeofenceTransitionService er IKKE startet");
        }

        if(AppUtils.isGooglePlayServicesAvailable(this)){
            statusText.append(" | Google Play Servicen er oppdatert og tilgjengelig");
        }else{
            statusText.append(" | Google Play Services er IKKE tilgjengelig med gammel utgave");
        }

        if(AppUtils.checkPermission(this)){
            statusText.append("\nVi har rettigheter til Lokasjon tilgjengelig");
        }else
        {
            AppUtils.askPermission(this);
            statusText.append("\nVi har IKKE rettigheter til Lokasjon tilgjengelig");
        }

        if(AppUtils.isGpsProviderAvailable(this)){
            statusText.append("\nLocation GPS provider er enabled");

        }else{
            statusText.append("\nLocation GPS provider er disabled");
           // Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
           // this.startActivity(settingsIntent);
        }

        if(AppUtils.isNetworkProviderAvailable(this)){
            statusText.append("\nLocation Network provider er enabled");
        }else
        {
            statusText.append("\nLocation Network provider er disabled");
        }

        if(googleApiClient.isConnected()){
            googleClientText.setText("googleApiClienten er connected");
        }else
        {
            googleClientText.setText("googleApiClienten er IKKE connected");
        }

        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1); //Nåværende battery level. fra 0 til scale.
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1); //Maximun battery level

        float batteryLevel = ((float)level / (float)scale) * 100.0f;
        if(level == -1 || scale == -1){

        }else
        {
            statusText.append("\n Battery nivå: " + batteryLevel + " %");
        }

       /* if(AppUtils.checkLocationAvailabillity(this,getGoogleApiClient())){
            statusText.append("\nVi har location tilgjengelig fra googleApiClient");
        }else
        {
            statusText.append("\nVi har IKKE location tilgjengelig fra googleApiClient");
        }
        */
        getScreenDimens();
        updateStartKioskGui();

        //TODO: dersom alt er OK her, og vi egentlig skal være i Kiosk, så må vi hoppe til den appen.
        if(PreferenceUtils.isKioskModeActivated(context)){
            PreferenceUtils.getPrefkioskModeApp(context);
        }

        super.onStart();
    }




    /**
     * Opdater Bruker grense snittet, slik at når vi gjør forandringer, så skal det synes her.
     */

    public void updateStartKioskGui(){
        if(PreferenceUtils.isKioskModeActivated(this)){
            //Må sette knappen til å være Disabled
            startKioskButton.setClickable(false);
            startKioskButton.setAlpha(0.5f); //greyer ut knappen litt.
            startKioskButton.setText("Kiosk mode is On");

        }else{
            startKioskButton.setClickable(true);
            startKioskButton.setAlpha(1); //greyer ut knappen litt.
            startKioskButton.setText("Start Kiosk Mode");
        }
    }

    public View touchView = null;
    public View navigationTouchView = null;

    public boolean isTouchView(){//denne returlere null uansett.
        WindowManager manager = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE));
        Log.d(TAG,"R.id.view_notification : "+this.findViewById(R.id.view_notification));
        Log.d(TAG,"R.id.view_notification : "+this.findViewById(R.id.view_notification));

        return touchView != null;
    }

    public boolean isTouchViewVisible(){
        if(isTouchView()) {
            //Dette ser ut til å kjøre før vi faktisk har laget viewet.
            Log.d(TAG,"visibility : "+ touchView.getVisibility());
            switch (touchView.getVisibility()) {

                //Synelig
                case View.VISIBLE:
                    return true;
                //usynelig, men plassen tar er forsatt ibruk. TODO: må finne ut om invisible kan forsatt ta imot touch events.
                case View.INVISIBLE:
                    return false;

                //Borte, kan ikke sees og tar ikke opp plass.
                case View.GONE:
                    return false;
            }
        }
        return false;
    }

    public void startConsumeView(){
        if(this.findViewById(R.id.view_notification) == null) {//vi legger bare till dersom det er null, som vill si at det ikke eksiterer allerede.

            WindowManager manager = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE));
            WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
            localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
            localLayoutParams.gravity = Gravity.TOP | Gravity.RIGHT; //Oppe til høyre vill denne komme opp

            localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    //Window flag: even when this window is focusable (its FLAG_NOT_FOCUSABLE is not set), allow any pointer events outside of the window to be sent to the windows behind it.
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |

                    //Window flag: place the window within the entire screen, ignoring decorations around the border (such as the status bar).
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            localLayoutParams.width = 960; //halvparten av sjermBredde, siden vi forsatt vill ha touch event-ene til notifikasjonene
            localLayoutParams.height = (int) (40 * getResources().getDisplayMetrics().scaledDensity); //Stod 50, så får se hvordan det går.
            localLayoutParams.format = PixelFormat.TRANSLUCENT; //litt gjennomsiktig. PixelFormat.TRANSLUCENT;
            CustomView view = new CustomView(this);
            //Bare slik at jeg kunne se Viewet.
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent_red));
            view.setId(R.id.view_notification);
            view.setAlpha(0.1f);
            touchView = view; //lagrere Viewet i en variabel;
            manager.addView(touchView, localLayoutParams);
        }else
        {
            Log.e(TAG, "Error med å legge til nytt NotifactionView");
        }

        /* //Bare en test på om det går ann å legge til et View over navigation baren, det fungerte i dette vindu, men når vi forlater her ifra. så er ikke lenger parent like stort.
        //WindowManager manager = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE));

        WindowManager.LayoutParams localLayoutParamsNavigation = new WindowManager.LayoutParams();
        localLayoutParamsNavigation.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        localLayoutParamsNavigation.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        localLayoutParamsNavigation.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                //Window flag: even when this window is focusable (its FLAG_NOT_FOCUSABLE is not set), allow any pointer events outside of the window to be sent to the windows behind it.
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |

                //Window flag: place the window within the entire screen, ignoring decorations around the border (such as the status bar).
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |

                WindowManager.LayoutParams.FLAG_FULLSCREEN;

        localLayoutParamsNavigation.width = 960;
        localLayoutParamsNavigation.height = (int) (60 * getResources().getDisplayMetrics().scaledDensity);
        localLayoutParamsNavigation.format = PixelFormat.TRANSLUCENT; //litt gjennomsiktig. PixelFormat.TRANSLUCENT
        CustomView navigationview = new CustomView(this);

        navigationview.setBackgroundColor(ContextCompat.getColor(context,R.color.transparent_red));
        navigationTouchView = navigationview;

        manager.addView(navigationTouchView,localLayoutParamsNavigation );
        */

    }

    //TODO fiks denne. For mye boilerplate kode.
    public void disableTouchView(){
        if(touchView != null){
            touchView.setVisibility(View.GONE);
        }
    }

    public void enableTouchView(){
        if(touchView != null){
            touchView.setVisibility(View.VISIBLE);
        }

    }

    private boolean toogle = true;

    public void toogleTouchView(View view){
        if(toogle){
            disableTouchView();
            toogle = false;
        }else {
            enableTouchView();
            toogle = true;
        }
    }

    @Override //Når vi holder inne Power Button, så kjører denne.
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getKeyCode() == KeyEvent.KEYCODE_POWER){
            Log.d(TAG,"Key power button pressed");
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onAttachedToWindow() {

        //this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        super.onAttachedToWindow();
    }

    public void startAccessibilityService(){
        Log.d(TAG,"startAccessibilityService");
        Intent accessibilityServiceIntent = new Intent(this,AccessibilityService.class);
        startService(accessibilityServiceIntent);

        if(! AppUtils.isServiceRunning(GeofenceTransitionService.class,this)){
            Intent GeofenceTransitionService = new Intent(context, GeofenceTransitionService.class);
            GeofenceTransitionService.setAction("STARt"); //slik at vi vet at det mobilen har blir resatt, vi må då muligens gå direkte til MonumentVandring.
            context.startService(GeofenceTransitionService);
        }
    }

    public void showApps(View v) {
        if(AppUtils.isAppInstalled(this,APP)){
            Toast.makeText(this, "Appen GeofencingAlpga er innstallert", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(this, "Appen Geofencing er ikke innstallert", Toast.LENGTH_SHORT).show();
        }
    }

    private void setKioskMode(boolean activate){
        UiModeManager uiModeManager = (UiModeManager) this.getSystemService(Context.UI_MODE_SERVICE);

        //TODO: sjekk at alt kjører. Viss ikke så må vi be brukeren starte opp noen ting.

       /* if(on){
            //uiModeManager.enableCarMode(0);
            //Log.d(TAG,"Car mode enabled flag:"+UiModeManager.ENABLE_CAR_MODE_GO_CAR_HOME);
        }else
        {
            //uiModeManager.disableCarMode(UiModeManager.DISABLE_CAR_MODE_GO_HOME);
        } */

        PreferenceUtils.setKioskModeActive(this, activate);
    }

    @Override
    protected void onResume() {
        //litt feil.
        if(allStatusTrueTest()){
            //dette betyr av vi egentlig skal gå til MonumentVandring.
            String prefApp = PreferenceUtils.getPrefkioskModeApp(context);
            Toast.makeText(this.getApplicationContext(), "Går til app med navn: "+prefApp, Toast.LENGTH_SHORT).show();
            Intent launcherIntent = getPackageManager().getLaunchIntentForPackage(prefApp);
            if(launcherIntent != null){
                startActivity(launcherIntent);
            }else{
                Log.e(TAG,"Error starting KioskMode fra Launcher");
            }
        }



        super.onResume();
        Log.d(TAG,"onResume()");

        hideSystemUiTest();
        setVisible(true);
    }

    public void getScreenDimens(){
        Configuration configuration = this.getResources().getConfiguration();
        int screenWidhtdp = configuration.screenWidthDp;
        int screenHeigthdp = configuration.screenHeightDp;
        Log.d(TAG,"Screen dimes width:"+screenWidhtdp+" dp heigth:"+screenHeigthdp+" dp");
    }

    public void hideSystemUiTest(){
        //decorView.setSystemUiVisibility(
                /*View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                */
        //);
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        );
    }

    @Override //Fra Google Developer på immersion
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //hideSystemUiTest();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG,"onBackPressed()");
        //super.onBackPressed(); //Siden vi er Bunnen av aplikasjons stacken, så er det ikke vits i å trykke onBack, den gjør heller ikke noe her ifra.
    }



    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);

        //activityManager.moveTaskToFront(getTaskId(),0);

        ComponentName cn = this.getComponentName();
        Log.d(TAG,cn.toString());

        //Husker ikke hva denne var for, muligens Recent button problemer.
        if(cn != null && ! cn.getClassName().equals(getClass().getName())){
            Log.d(TAG, "CN true, er recent button");
            activityManager.moveTaskToFront(getTaskId(), 0);
        }
    }

    public Context getContext(){
        if(context != null)
            return context;
        else
            return this.getApplicationContext();
    }

    public void startMap(View view){
        Intent intent = new Intent();
        intent.setAction("com.sondreweb.geofencingalpha");
        //context.startActivity(intent);
    }

    //TODO: Bruke en App object istedet, litt tryggere på errors.
    public void StartApp(String packageName){
        Log.d(TAG,"StartApp: "+ packageName);
        Intent intent = this.getPackageManager().getLaunchIntentForPackage(packageName);

        if(intent != null ){
            startActivity(intent);
        }
    }

    //starter Kiosk mode
    /*
    *   Denne skal skru på KioskMode, og så skal vi hoppe til MonumentVandring. Det er forusatt at alt er klart.
    * */
    public void startKioskMode(View view){
        if( ! kioskModeReady() ){
            //Da kan vi ikke starte KioskModen.
        }

        setKioskMode(true);
        //startKioskButton.setLayoutParams(new RelativeLayout.LayoutParams(100,100));
        updateStartKioskGui();
        //TODO: Hopp til MonumentVandring

        Intent intent = getPackageManager().getLaunchIntentForPackage(PreferenceUtils.getPrefkioskModeApp(context));
        if(intent != null){
            this.startActivity(intent);
        }else{ //dersom intent er null, så har vi et problem
            Log.e(TAG,"Error ved å starte kioks_mode appen");
        }
    }

    /*
     Denne skal returnere true, dersom alt av krav er oppfylt.
        Accessibility service må være på.
        Touch Event View som blokker quick settings.
        MonumentVandring appen må også være innstalert og satt og korrekt(med monumentene lastet inn).
    * */
    public boolean kioskModeReady(){
        if(AppUtils.isAccessibilitySettingsOn(this)){
            return true;
        }else {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(intent, 0);
            return false;
        }
    }

    /*  Start Loging Activity for admin users.
    *   Ikke ferdig.
    *
    * */
    public void startAdminLogin(View view) {
        Intent intent = new Intent(this, LoginAdminActivity.class);
        startActivity(intent);
    }

    /*
    *   Callback for når vi requester permission for å hente ut lokasjon.
    * */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case AppUtils.REQ_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //getLastKnowLocation();
                } else {
                    //permissionDenied();
                    //Siden vi ikke har permission til å bruke Location, så kan vi heller ikke starte monument vandring eller Geofence etc.
                    //TODO: stop app her.
                }
                break;
            }
        }
    }

    /*                                 CALLBACKS                                        */
    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤GoogleApiClient.ConnectionCallbacks¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤*/

    /**
     *  Når Google Clienten connecter successfully kjøres denne.
     */

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG,"Vi er Connected med GoogleAPiClienten");
        googleClientText.setText("googleApiClienten er connected");
    }

    /**
     * Når Google Client minster forbindelsne kjørere denne.
     */

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG,"Mister connection med GoogleApiClienten");
        googleClientText.setText("googleApiclienten: onConnectionSuspended");
    }

    /*#######################GoogleApiClient.ConnectionCallbacks#######################*/
    /*                                   END                                           */

    /*                                 CALLBACKS                                        */
    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤GoogleApiClient.OnConnectionFailedListener¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤*/

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG,"Failed to connect googleAPiClient"); //Muligens Gammel Versjon av
        //TODO: Finn ut hva problemet kan være og koble til på nytt.

        //TODO: Be bruker oppdatere GooglePlayService, dersom gammel versjon.
        googleClientText.setText("googleApiClienten on ConnectionFailed");
    }

/*#######################END oogleApiClient.OnConnectionFailedListener###############*/

    /* GET FUNCTIONS */
    public GoogleApiClient getGoogleApiClient(){
        if(googleApiClient != null){
            return googleApiClient;
        }
        this.createGoogleApi();
        return googleApiClient;
    }
    /* SET FUNCTIONS*/

    public void stopKioskMode(View view){
        PreferenceUtils.setKioskModeActive(this,false);
        updateStartKioskGui();
    }

    public void lockScreenNow(View view){
        if(PreferenceUtils.isAppDeviceAdmin(this)) {
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.getSystemService(Context.DEVICE_POLICY_SERVICE);
            devicePolicyManager.lockNow();
        }
    }

    public void unlockScreenNow(){
        if(PreferenceUtils.isAppDeviceAdmin(this)) {
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.getSystemService(Context.DEVICE_POLICY_SERVICE);
        }
    }
}

