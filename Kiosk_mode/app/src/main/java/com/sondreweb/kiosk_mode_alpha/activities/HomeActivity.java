package com.sondreweb.kiosk_mode_alpha.activities;

import android.app.ActivityManager;
import android.app.UiModeManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
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

import com.google.android.gms.common.api.GoogleApiClient;
import com.sondreweb.kiosk_mode_alpha.CustomView;
import com.sondreweb.kiosk_mode_alpha.classes.StatusInfo;
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
        ActivityCompat.OnRequestPermissionsResultCallback{

    public final static String TAG = HomeActivity.class.getSimpleName();
    private final static String APP = "com.sondreweb.geofencingalpha";


    final float BATTERY_LIMIT = 80f; //hvor mange proset batteriet må minst være på.
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

    private BatteryBroadcastReceiver batteryBroadcastReceiver;

    private StatusAdapter statusAdapter;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        //this.startActivity(intent);

        gridView = (GridView)findViewById(R.id.grid_status_view);

        startKioskButton = (Button) findViewById(R.id.button_start_kiosk);

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


        batteryBroadcastReceiver = new BatteryBroadcastReceiver();

        //lager statusAdapteret vi trenger til senere.
        statusAdapter = new StatusAdapter(HomeActivity.this);


        gridView.setOnItemClickListener(new OnStatusItemClickListener());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
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

    /* ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤STATUS¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *   Lager en Liste med Statuser, som inneholder navn og en bool
    *   hvor boolen forteller om statusen er klar.
    *   Når alt er klart kan vi begynne Vandringen(MonumentVandring).
    * */

    private final String googlePlayServiceStatus = "Google Play Service",
            deviceAdminStatus = "Device Admin",
            accessibilityServiceStatus = "Accessibility Service",
            touchViewStatus = "Quick Settings Restriction",
            locationEnabledStatus= "Location",
            batteryStatus = "Battery",
            homeStatus = "Home";

    public void createAndUpdateStatusList(){
        if(! statusList.isEmpty()){//dersom den ikke er tom.
            statusList = new ArrayList<>(); //tømmer listen.
        }
        /*
        *   Tester med ulike settings.
        * */



        StatusInfo status; //status objekt som vi bare kan bruke, mulig dette er litt dårlig, siden vi får samme objekt med ulike parametere.



        status = new StatusInfo(googlePlayServiceStatus);
        if(AppUtils.isGooglePlayServicesAvailable(this)){
            status.setStatus(true);
        }else{
            status.setStatus(false);
        }
        statusList.add(status);

        status = new StatusInfo(deviceAdminStatus);
        if(PreferenceUtils.isAppDeviceAdmin(this)){
            status.setStatus(true);
        }else{
            status.setStatus(false);
        }
        statusList.add(status);

        status = new StatusInfo(accessibilityServiceStatus);
        if(AppUtils.isAccessibilitySettingsOn(this)){
            status.setStatus(true);
        }else{
            status.setStatus(false);
        }
        statusList.add(status);

        //sjekker touchVievet
        status = new StatusInfo(touchViewStatus);
        if(isTouchViewVisible()){
            status.setStatus(true);
        }else{
            status.setStatus(false);
        }
        statusList.add(status);


        //Må sjekke at Location er enabled
        status = new StatusInfo(locationEnabledStatus);
        if(AppUtils.checkLocationPermission(this)){
            status.setStatus(true);
        }else{
            status.setStatus(false);
        }

        statusList.add(status);


        /*
        *   Dette kan ha problemer med tanke på at under bruk vill vi stupe under.
        * */
        //Må sjekke at Location er enabled
        status = new StatusInfo(batteryStatus);
        if(HomeActivity.getLevel() >= BATTERY_LIMIT ){
            status.setStatus(true);
        }else{
            status.setStatus(false);
        }
        statusList.add(status);

        /*
        *   Status på om vi er HOME launcher.
        * */

        status = new StatusInfo(homeStatus);
        if(AppUtils.isDefault(this,this.getComponentName())){
            status.setStatus(true);
        }else{
            status.setStatus(false);
        }
        statusList.add(status);

        /*
        *   Bare masse testinger.
        * */

        status = new StatusInfo("Status 1");
        status.setStatus(true);
        statusList.add(status);

        status = new StatusInfo("Status 2");
        status.setStatus(true);
        statusList.add(status);

        status = new StatusInfo("Status 3");
        status.setStatus(true);
        statusList.add(status);

        status = new StatusInfo("Status 4");
        status.setStatus(true);
        statusList.add(status);
        status = new StatusInfo("Status ...");
        status.setStatus(true);
        statusList.add(status);

        status = new StatusInfo("Status n ");
        status.setStatus(true);
        statusList.add(status);



        Log.d(TAG, statusList.toString());


        if(!statusAdapter.isEmpty()){ //Må tømme AraryAdaptere for items, dersom vi gjør forandringer i ArrayListen.
            statusAdapter.clear();
        }

        statusAdapter.setData(statusList);
        gridView.setAdapter(statusAdapter);
    }


    private final String AccessibilitySettings = Settings.ACTION_ACCESSIBILITY_SETTINGS;
    private final String DeviceAdminSettings = Settings.ACTION_SECURITY_SETTINGS;
    private final String DeviceAdminSettingsTest = "com.android.settings.DeviceAdminSettings";
    private final String HomeSettings = Settings.ACTION_HOME_SETTINGS;
    private final String LocationSettings = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
    /*
    *   OnItemClickListnener for GridViewet med statuser.
    * */
    public class OnStatusItemClickListener implements AdapterView.OnItemClickListener{
        private final String TAG = OnStatusItemClickListener.class.getSimpleName();

        StatusInfo statusInfo = null;




        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //TODO: gjør de ulike tingene.
            Log.d(TAG, parent.getItemAtPosition(position).getClass().toString() );
            try {
                statusInfo = (StatusInfo) parent.getItemAtPosition(position);
                //Tilfelle det er feil klasse, men dette skal strengt tatt aldri skje, siden adaptere kunn legger ut StatusInfo objecter.
            }catch (ClassCastException e){
                Log.e(TAG,e.getLocalizedMessage());
            }
            //TODO gå til de ulike klassene.
            switch (statusInfo.getName()){
                case googlePlayServiceStatus:
                    break;
                case deviceAdminStatus:

                    break;
                case accessibilityServiceStatus:
                    startActivity(new Intent(AccessibilitySettings));

                    break;
                case touchViewStatus:
                    toogleTouchView();
                    break;
                case locationEnabledStatus:

                    break;
                case batteryStatus:

                    break;
                case homeStatus:
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                        startActivity(new Intent(HomeSettings));
                    }else{
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    }

                    break;
            }
            //Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);

            //enten så må vi sendes til settings.
            if(statusInfo.getName().equalsIgnoreCase("TouchView")){
                //dersom vi har trykket på TouchView så må vi toggle TouchViewet.
                //toogleTouchView();
            }

            //eller vi må simpelten skru på noe.

            //Må gjøre en oppdatering på Statuslisten.
            //Siden vi ikke faktisk gør en forandring på Arrayet, som er egentlig det vi burde.

            createAndUpdateStatusList();

        }

    }



    //TODO: logikk på om vi er klare til å starte.
    //Noen ting kan vi faktisk bare sette på selv, som TouchViewet ivertfall.

    public boolean allStatusTrue(){
        for(StatusInfo s : statusList){
            if(! s.getStatus()){ //dersom det failer en gang, så er det ikke klart til å starte.
                return false;
            }
        }
        return true;
    }

    public boolean allStatusTrueTest(){
        Log.d(TAG,"PrefUtils.isKiosk: "+PreferenceUtils.isKioskModeActivated(context));
        Log.d(TAG,"AppUtils.isAccesibillitySettingsOn: "+AppUtils.isAccessibilitySettingsOn(context));


       if(PreferenceUtils.isKioskModeActivated(context) && AppUtils.isAccessibilitySettingsOn(context) && AppUtils.isDefault(context,this.getComponentName() )){
           //Dersom Accessibility service er på og KioskModeActivated og isDefault, så kan vi faktisk starte.
           return true;
       }
        return false;
    }

    private void startPrefKioskModeApp(){ //Er noe galt her...
        if(PreferenceUtils.isKioskModeActivated(context)) {//vi skal bare gjøre noe dersom KiosMode er satt til True.
            String prefApp = PreferenceUtils.getPrefkioskModeApp(context);
            Log.d(TAG, "prefApp: " + prefApp.toString());
            Intent launcherIntent = getPackageManager().getLaunchIntentForPackage(prefApp);
            try {
                Log.v(TAG,"Går til app med navn " + prefApp+" ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                Log.d(TAG, "launcherIntent: " + launcherIntent);
                //Denne er null...
                startActivity(launcherIntent);
               //startActivityForResult(launcherIntent,0);
            } catch (NullPointerException e) {
                Log.e(TAG, e.getMessage());
            }

        }else{
            Log.d(TAG,"Kiosk mode er off");
        }
    }

    static final int PICK_CONTANCT_RQEUST = 1;



    @Override
    protected void onStart() {//Ved onstart burde vi sjekke ulike ting.
        Log.d(TAG,"onStart()");
        if(allStatusTrueTest()){
            //dette betyr av vi egentlig skal gå til MonumentVandring.
            startPrefKioskModeApp();
        }

        //Registerer BroadcastRecievern for battery med IntentFilter ACTION_BATTERY_CHANGED.
        Intent intent = registerReceiver(batteryBroadcastReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        //Henter ut nåværende verdi om batteriet.
        try {
            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1); //Nåværende battery level. fra 0 til scale.
            scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1); //Maximun battery level
        }catch (NullPointerException e){
            Log.e(TAG, e.toString());
        }



        createAndUpdateStatusList(); //oppdaterer listen.

        if(AppUtils.isServiceRunning(GeofenceTransitionService.class,this)){
            statusText.setText("GeofenceTransitionService kjører");
        }else{
            statusText.setText("GeofenceTransitionService er IKKE startet");
        }

        if(AppUtils.isGooglePlayServicesAvailable(this)){
            statusText.append(" | Google Play Servicen er oppdatert og tilgjengelig");
        }else{
            statusText.append(" | Google Play Services er IKKE tilgjengelig med gammel utgave");
        }

        if(AppUtils.checkLocationPermission(this)){
            statusText.append("\nVi har rettigheter til Lokasjon tilgjengelig");
        }else
        {
            AppUtils.askLocationPermission(this);
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

        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1); //Nåværende battery level. fra 0 til scale.
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1); //Maximun battery level

        float batteryLevel = ((float)level / (float)scale) * 100.0f;
        if(level == -1 || scale == -1){
            //error tror jeg?
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

    @Override
    protected void onResume() {
        //litt feil.
        createAndUpdateStatusList();
        Log.d(TAG,"onResume() i Home");
        if(allStatusTrueTest()){
            //dette betyr av vi egentlig skal gå til MonumentVandring.
            startPrefKioskModeApp();
        }

        Log.d(TAG,"Forbi startActivity %:%:%:%:%:%:%:%:%");

        super.onResume();
        //createAndUpdateStatusList();
        Log.d(TAG,"onResume()");

        hideSystemUiTest();
        setVisible(true);
    }

    @Override
    protected void onRestart() {
        if(allStatusTrueTest()){
            //dette betyr av vi egentlig skal gå til MonumentVandring.
            startPrefKioskModeApp();
        }
        super.onRestart();
    }

    SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            //TODO: oppdater GUI når noe forandres her.
            switch (key){
                case PreferenceUtils.PREF_KIOSK_MODE:
                    updateStartKioskGui();
            }
        }
    };

    @Override
    protected void onPostResume() {
        //Kan vi kanskje hoppe her ifra istede?
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
        if(allStatusTrueTest()){
            //dette betyr av vi egentlig skal gå til MonumentVandring.
            startPrefKioskModeApp();
        }
        super.onPostResume();
    }


    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);

        //TODO: queryBroadcastReceivers
        try { //unregister reciever dersom den forsatt er registert.
            unregisterReceiver(batteryBroadcastReceiver); //unregisterer recievern, siden vi ikke er interesert i batteri nivået lenger.
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        }catch(IllegalArgumentException e){
            Log.e(TAG,e.getMessage());
        }
        //activityManager.moveTaskToFront(getTaskId(),0);

        ComponentName cn = this.getComponentName();
        Log.d(TAG,cn.toString());

        //FIXME 2131:2313.123
        //Husker ikke hva denne var for, muligens Recent button problemer.
        if(! cn.getClassName().equals(getClass().getName())){
            Log.d(TAG, "CN true, er recent button");
            activityManager.moveTaskToFront(getTaskId(), 0);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG,"????????????????????Resultcode: "+resultCode);
        if(resultCode == RESULT_CANCELED){
            //VI må starte aktiviteten på nytt?
            startActivity(getPackageManager().getLaunchIntentForPackage(PreferenceUtils.getPrefkioskModeApp(context)));
        }

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

    public boolean isTouchView(){//denne
        WindowManager manager = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE));

        return touchView != null;
    }

    public boolean isTouchViewVisible(){
        if(isTouchView()) {
            //Dette ser ut til å kjøre før vi faktisk har laget viewet.
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
        }
        else
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

    public void toogleTouchView(){
            //Sjekker at touchViewet vårt ikke er tomt.

        if(touchView != null){
            if(touchView.getVisibility() == View.GONE){
                touchView.setVisibility(View.VISIBLE);
            }else {
                touchView.setVisibility(View.GONE);
            }
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
            Intent GeofenceServiceIntent = new Intent(context, GeofenceTransitionService.class);
            GeofenceServiceIntent.setAction(GeofenceTransitionService.START_SERVICE); //slik at vi vet at det mobilen har blir resatt, vi må då muligens gå direkte til MonumentVandring.

            Log.d(TAG,"Starting Service GeofenceService......................................");
            context.startService(GeofenceServiceIntent);
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

        PreferenceUtils.setKioskModeActive(this, activate);
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
        if(allStatusTrueTest()){
            //dette betyr av vi egentlig skal gå til MonumentVandring.
            startPrefKioskModeApp();
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
    *   Knappen som brukeren har tilgang til.
    * */
    public void startKioskMode(View view){
        if( ! kioskModeReady() ){
            //Da kan vi ikke starte KioskModen.
            return;
        }

        setKioskMode(true);
        //startKioskButton.setLayoutParams(new RelativeLayout.LayoutParams(100,100));
        updateStartKioskGui();

        //TODO: Hopp til MonumentVandring

        startPrefKioskModeApp();
    }

    /*
     Denne skal returnere true, dersom alt av krav er oppfylt.
        Accessibility service må være på.
        Touch Event View som blokker quick settings.
        MonumentVandring appen må også være innstalert og satt og korrekt(med monumentene lastet inn).
    * */
    public boolean kioskModeReady(){//KioskMode er ikke ready
        if(AppUtils.isAccessibilitySettingsOn(this)){
            return true;
        }else {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(intent,0);
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
                    //Siden vi ikke har permission til å bruke Location, så kan vi heller ikke starte monument vandring eller GeofenceClass etc.
                    //TODO: stop app her.
                }
                break;
            }
        }
    }

    /* SET FUNCTIONS*/

    public void lockScreenNow(View view){
        if(PreferenceUtils.isAppDeviceAdmin(this)) {
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.getSystemService(Context.DEVICE_POLICY_SERVICE);
            devicePolicyManager.lockNow();
        }
    }

    //Går ikke...
    public void unlockScreenNow(){
        if(PreferenceUtils.isAppDeviceAdmin(this)) {
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.getSystemService(Context.DEVICE_POLICY_SERVICE);
        }
    }

    /*
    *   Broadcast reciever
    * */
    public static int level = -1;
    public static int scale = -1;
    public String id;

    //Regn ut batteri nivået i prosent.
    public static float getLevel(){
        float batteryLevel = ((float)level / (float)scale) * 100.0f;
        if(level == -1 || scale == -1){
            //error tror jeg?
            return 0;
        }else
        {
            return batteryLevel;
        }

    }

    //Indre classe som tar hånd om å hente ut batterinivået når det forandres.
    /*
     *  IntentFilter(Intent.ACTION_BATTERY_CHANGED)
     */

    public class BatteryBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            //denne skal oppdatere variablene våre, når det er nødvendig.
            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1); //Nåværende battery level. fra 0 til scale.
            scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1); //Maximun battery level
            Log.d(TAG+"Battery", "level: "+((float)level/(float)scale*100.0f) );

            //Oppdaterer GridView listen.
            createAndUpdateStatusList();

            //TODO: update status listen.
        }
    }
}

