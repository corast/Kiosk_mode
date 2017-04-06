package com.sondreweb.kiosk_mode_alpha.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.sondreweb.kiosk_mode_alpha.services.AccessibilityService;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by sondre on 03-Mar-17.
 *
 * Globale funksjoner som kan brukes flere steder i systemet.
 */

public class AppUtils{

    private static final String TAG = AppUtils.class.getSimpleName()+"_custom";


    public final static int REQ_PERMISSION = 200;

    public AppUtils(){ //tom konstruktør
    }

    //brukt for å finne ut om Servicen vår kjører i bakgrunn eller ikke.
    public static boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo serviceInfo : actManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(serviceInfo.service.getClassName())) {
                Log.i("isMyServiceRunning? ", true + "");
                return true;
            }
        }
        Log.i("isMyServiceRunning?", false + "");
        return false;
    }



    public static boolean isGooglePlayServicesAvailableAndPoll(Context context, Activity activity){
        Log.d(TAG,"isGooglePlayServicesAvaliable");
        GoogleApiAvailability googleApiAvailbility = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailbility.isGooglePlayServicesAvailable(context);
        if( resultCode != ConnectionResult.SUCCESS){
            //gir brukeren beskjed om hvorfor det ikke gikk
            //ConnectionResult.RESOLUTION_REQUIRED vill si at vi kan be brukeren intallerer hva det som mangler.
            try {
                if (googleApiAvailbility.isUserResolvableError(resultCode)) {

                    if(apkFileExists(context,"/download/com.google.android.gms.apk")){

                    }

                    if(installFromApk(context, "/download/com.google.android.gms.apk")){
                        //installFromApk(context,"/download/com.google.android.gms.apk");
                    }

                    if(isPacketInstalled("com.google.android.gms.apk",context)){
                        Log.d(TAG, "Package is not installed.");
                    }


                    resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context); //oppdaterer.
                    if( resultCode != ConnectionResult.SUCCESS){
                       // googleApiAvailbility.getErrorDialog(activity, resultCode, 2404).show();
                    }
                    //googleApiAvailbility.getErrorDialog(activity, resultCode, 2404).show(); //ber brukeren installere Google Play Services fra Play Store.
                }
            }catch (Exception e) {
                Log.e("Error"+TAG," "+e);
            }
        }
        return resultCode == ConnectionResult.SUCCESS; //returnere kunn true dersom ConnectionResult.SECCESS som o
    }

    /*
    *   ConnectionResult.SUCCESS vill si vi koblet oss velykket til. '
    *   Merk at dette tar ikke i betraktning om vi trenger en oppdatering av Google Play Services.
    *       men viss det er SUCCESS så er versjonen god nok.
    * */

    public static boolean isGooglePlayServicesAvailable(Context context){
        GoogleApiAvailability googleApiAvailbility = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailbility.isGooglePlayServicesAvailable(context);
        return resultCode == ConnectionResult.SUCCESS;
    }

    public static boolean isPacketInstalled(String packagename, Context context){
        try{
            context.getPackageManager().getPackageInfo(packagename,0);
            return true;
        }catch (PackageManager.NameNotFoundException e){
            return false;
        }
    }

    /*
    *   Metode for å be brukeren installerer APK fra filsti.
    * */
    private static boolean installFromApk(Context context, String apkFile){
        Log.d(TAG, Environment.getExternalStorageDirectory() + apkFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        try {
            /*
            * Prøver å starte en ny activitet, som er en poppup fragment med informasjon om hva som skal innstalleres, og om dette gotas av brukern.
            * Men brukeren kan avslå dette, og ingen ting skjer.
            * */
            intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + apkFile)), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

        }catch (NullPointerException E){
            return false;
        }catch (Exception E){
            return false;
        }
        return true;
    }


    /*
    * Skal sjekke om en APK fil eksitere på valg posisjon, men var noe problematisk.
    * */
    public static boolean apkFileExists(Context context, String filePath){
        File file = new File(Environment.getExternalStorageDirectory() + filePath);
        Log.d(TAG,"Path : "+Environment.getExternalStorageDirectory() + filePath);


        if(file.isFile()){
            Log.d(TAG,filePath+ " exists");
            return true;
        }
        return false;
    }


    /*
    *   Sjekker om Accessibility servicen er på.
    *
    *   Fra code siden....
    * */
    public static boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        //Navn på servicen, pakkenavn + navn på servicen.
        final String service = mContext.getPackageName() + "/" + AccessibilityService.class.getCanonicalName();
        try {
            //henter int verdien på om den er enabled.
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);

            //Log.v(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error finding setting, default accessibility to not found: "
                    + e.getMessage());
        }

        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            //Log.v(TAG, "***ACCESSIBILITY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    //Log.v(TAG, "-------------- > accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        //Log.v(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            Log.v(TAG, "***ACCESSIBILITY IS DISABLED***");
        }

        return false;
    }



    public static boolean isDeviceOwner(Context context){
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return devicePolicyManager.isDeviceOwnerApp(context.getPackageName());
    }


    //Sjekker om vi har permission for å få tak i FINE_LOCATION (accurate location), Dette vill si at vi kan bruke GPS og WIFI for å finne lokasjon.
    public static boolean checkLocationPermission(Context context) {
        boolean permissionGranted = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "checkLocationPermission() result: "+permissionGranted);
        return permissionGranted;
    }

    //spør om permission viss det ikke er gitt på å få tak i Fine location.
    public static void askLocationPermission(Activity activity) {
        Log.d(TAG, "askLocationPermission()");
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQ_PERMISSION
        );
    }


 /*
 * returns true, then the location returned by getLastLocation(GoogleApiClient) will be reasonably up to date within the hints specified by the active LocationRequests.
 *  If the client isn't connected to Google Play services and the request times out, null is returned.
 * */
    //sjekker GoogleApiClienteten om den faktisk klarere p hente Location.
    public static boolean checkLocationAvailabillity(Context context,GoogleApiClient googleApiClient) { //Denne returnere true dersom vi har Location enabled og faktisk driver å henter locationfra google clientent.
        Log.d(TAG,"checkLocationAvailabillity "+ LocationServices.FusedLocationApi.getLocationAvailability(googleApiClient).isLocationAvailable());
        if(checkLocationPermission(context)) { //sjekker om vi har rettigheter først og fremst(forhindrer Error dersom vi har mistet de underveis av en eller annen grunn).
            return LocationServices.FusedLocationApi.getLocationAvailability(googleApiClient).isLocationAvailable();
            //sjekker location er tilgjenngelig.
        }
        return false;
    }

    //Avalible
    public static boolean isProvidersAvailable(Context context){
       // Log.d(TAG,"isGooglePlayServiceAvalible: true");
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        //sjekker om GPS og Nettverk er påskrudd i enheten. WIfi kan være avskrudd, men ikke mobilnett.
        if( ! manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && ! manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){ //verken GPS eller WIFI er enabled.
            //dersom GPS provicer og Network provider er enabled(ikke avskrudd)
            Toast.makeText(context, "Verken GPS eller WIFI er på.", Toast.LENGTH_SHORT).show();
            return false;
        }else if( ! manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){ //At kunn GPS er enabled, bør være greit i første omgang.
            Toast.makeText(context, "Enable location services(GPS) for accurate data", Toast.LENGTH_SHORT).show();
            return false;
        }else if( ! manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){ //Wifi trenger ikke være enabled i første omgang.
            Toast.makeText(context, "Enable location services(WIFI) for accurate data", Toast.LENGTH_SHORT).show();
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            //context.startActivity(settingsIntent);
            return false;
        }
        else
        {
            //Log.d(TAG, "Location Enabled");
            return true;
        }
    }

    public static boolean isNetworkProviderAvailable(Context context){
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);


        if( locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER )){
            return true;
        }
        return false;
    }

    public static boolean isGpsProviderAvailable(Context context){
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if( locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER )){
            return true;
        }
        return false;
    }


    /*Sjekekr om appen er innstalert, vi må altså sjekke om Monumentvandrings appen er installert, ellers så får ikke brukeren gjordt noe.*/
    public static boolean isAppInstalled(Context context, String uri){
        try{
            context.getPackageManager().getApplicationInfo(uri,0);
            return true;
        }
        catch (PackageManager.NameNotFoundException e){
            return false;
        }
    }

    /*
    *   Boilerplate code for å håndtere at ulike settings ikke er skrudd på.
    *
    *   Dette gjelder kunn for DeviceManager, AccessibilityService, Location GPS og WIFI, Google Play Services, TouchView
    * */


    public static boolean isNotificationMenuLocked(){ //denne må sjekke om touchEvent Viewet eksisterer og er synelig.
        return true;
    }

    public float getBatteryLevel(Context context){
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1); //Nåværende battery level. fra 0 til scale.
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1); //Maximun battery level

        float batteryLevel = ((float)level / (float)scale) * 100.0f;
        if(level == -1 || scale == -1){
            //error tror jeg?
            return 0;
        }else
        {
            return batteryLevel;
            //statusText.append("\n Battery nivå: " + batteryLevel + " %");
        }

    }

    public static boolean isDefault(Context context,ComponentName component) {
        ArrayList<ComponentName> components = new ArrayList<ComponentName>();
        ArrayList<IntentFilter> filters = new ArrayList<IntentFilter>();
        context.getPackageManager().getPreferredActivities(filters, components, null);
        return components.contains(component);
    }

/*
    public static boolean canWriteSettings(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent, 200);
                context.startActivityForResult
                return true;
            }
        }
        return false;
    } */



}
