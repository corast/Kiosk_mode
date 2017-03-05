package com.sondreweb.kiosk_mode_alpha.activities;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.sondreweb.kiosk_mode_alpha.deviceAdministator.DeviceAdminKiosk;
import com.sondreweb.kiosk_mode_alpha.services.GeofenceTransitionService;
import com.sondreweb.kiosk_mode_alpha.utils.AppUtils;
import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;
import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.services.TestAccessiblityService;

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

    private TextView statusText;

    private View decorView;
    private PackageManager packageManager;
    private Context context;

    private DevicePolicyManager devicePolicyManager;

    //vi holder på googleApiClienten vår her.
    private static GoogleApiClient googleApiClient;


    static {
        //System.loadLibrary("");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


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

        if(devicePolicyManager.isDeviceOwnerApp(getPackageName())){
           Log.d(TAG, "Vi er Device owner");
        }
        else
        {
            Log.d(TAG, "Vi er ikke Device owner");
        }

        decorView = getWindow().getDecorView();
        context = this;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        startAccessibilityService();
        //checkIfAppInstalled(this, "testing");
    }

    @Override
    protected void onStart() {
        if(PreferenceUtils.isAppDeviceAdmin(this)){
            statusText.setText("Vi er device admin");
        }else{
            statusText.setText("Vi er IKKE device admin");
        }

        if(devicePolicyManager.isDeviceOwnerApp(this.getPackageName())){
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

        if(AppUtils.isGooglePlayServicesAvaliable(this,this)){
            statusText.append(" | GooglePlaySerevices er tilgjengelig");
        }else{
            statusText.append(" | GooglePlaySerevices er IKKE tilgjengelig");
        }

        if(AppUtils.checkPermission(this)){
            statusText.append("\nVi har rettigheter til Lokasjon tilgjengelig");
        }else
        {
            AppUtils.askPermission(this);
            statusText.append("\nVi har IKKE rettigheter til Lokasjon tilgjengelig");
        }

        if(AppUtils.isProvidersAvailable(this)){
            statusText.append("\nVi har ikke Location providerene Enabled på systemet");
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            this.startActivity(settingsIntent);
        }else{
            statusText.append("\nLocation GPS og NETWORK providerene er enabled");
        }

       /* if(AppUtils.checkLocationAvailabillity(this,getGoogleApiClient())){
            statusText.append("\nVi har location tilgjengelig fra googleApiClient");
        }else
        {
            statusText.append("\nVi har IKKE location tilgjengelig fra googleApiClient");
        }
        */


        super.onStart();
    }


    public void startAccessibilityService(){
        Log.d(TAG,"startAccessibilityService");
        Intent accessibilityServiceIntent = new Intent(this,TestAccessiblityService.class);
        startService(accessibilityServiceIntent);

        if(! AppUtils.isServiceRunning(GeofenceTransitionService.class,this)){
            Intent GeofenceTransitionService = new Intent(context, GeofenceTransitionService.class);
            GeofenceTransitionService.setAction("STARt"); //slik at vi vet at det mobilen har blir resatt, vi må då muligens gå direkte til MonumentVandring.
            context.startService(GeofenceTransitionService);
        }
    }

    public void showApps(View v) {

        if(checkIfAppInstalled(this,APP)){
            Toast.makeText(this, "Appen GeofencingAlpga er innstallert", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(this, "Appen Geofencing er ikke innstallert", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume()");
        hideSystemUiTest();
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
    }


    @Override
    public void onBackPressed() {
        Log.d(TAG,"onBackPressed()");
        //super.onBackPressed(); //Siden vi er Bunnen av aplikasjons stacken, så er det ikke vits i å trykke onBack, den gjør heller ikke noe her ifra.
    }

    /*Sjekekr om appen er innstalert, vi må altså sjekke om Monumentvandrings appen er installert, ellers så får ikke brukeren gjordt noe.*/
    public boolean checkIfAppInstalled(Context context, String uri){
        try{
            context.getPackageManager().getApplicationInfo(uri,0);
            return true;
        }
        catch (PackageManager.NameNotFoundException e){
            return false;
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);

        //activityManager.moveTaskToFront(getTaskId(),0);
    }

    public Context getContext(){
        if(context != null)
            return context;
        else
            return this.getApplicationContext();
    }

    public void startMonumentVandring(View view) {
       //vi starter MonumentVandringen
        PreferenceUtils.setKioskModeActive(true, this);
        StartApp(APP);
    }

    public void startMap(View view){
        Intent intent = new Intent(this,MapsActivity.class);
        startActivity(intent);
    }

    //TODO: Bruke en App object istedet, litt tryggere på errors.
    public void StartApp(String packageName){
        Log.d(TAG,"StartApp: "+ packageName);
        Intent intent = this.getPackageManager().getLaunchIntentForPackage(packageName);

        if(intent != null ){
            startActivity(intent);
        }
    }

    /*  Start Loging Activity for admin users.
    * */
    public void startAdminLogin(View view) {
        Intent intent = new Intent(this, LoginAdminActivity.class);
        startActivity(intent);
    }

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

    }

    /**
     * Når Google Client minster forbindelsne kjørere denne.
     */

    @Override
    public void onConnectionSuspended(int i) {

    }

    /*#######################GoogleApiClient.ConnectionCallbacks#######################*/
    /*                                   END                                           */

    /*                                 CALLBACKS                                        */
    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤GoogleApiClient.OnConnectionFailedListener¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤*/

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //TODO: Finn ut hva problemet kan være og koble til på nytt.
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

    //Create GoogleApiClient Instance
    private void createGoogleApi() {
        Log.d(TAG, "createGoogleApi()");
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            Log.d(TAG,googleApiClient.toString());
        }
    }

    public void lockScreenNow(View view){
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.getSystemService(Context.DEVICE_POLICY_SERVICE);
        devicePolicyManager.lockNow();
    }

}

