package com.sondreweb.kiosk_mode_alpha.utils;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.sondreweb.kiosk_mode_alpha.services.TestAccessiblityService;

/**
 * Created by sondre on 03-Mar-17.
 *
 * Globale funksjoner som kan brukes flere steder i systemet.
 */

public class AppUtils{

    private static final String TAG = AppUtils.class.getSimpleName();

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

    public static boolean isGooglePlayServicesAvaliable(Context context, Activity activity){
        //Log.d(TAG,"isGooglePlayServicesAvaliable");
        GoogleApiAvailability googleApiAvailbility = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailbility.isGooglePlayServicesAvailable(context);
        if( resultCode != ConnectionResult.SUCCESS){
            //gir brukeren beskjed om hvorfor det ikke gikk
            try {
                if (googleApiAvailbility.isUserResolvableError(resultCode)) {
                    googleApiAvailbility.getErrorDialog(activity, resultCode, 2404).show();
                }
            }catch (Exception e) {
                Log.e("Error"+TAG," "+e);
            }
        }
        return resultCode == ConnectionResult.SUCCESS;
    }

    // To check if service is enabled
    public static boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = mContext.getPackageName() + "/" + TestAccessiblityService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.v(TAG, "accessibilityEnabled = " + accessibilityEnabled);
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


    //Sjekker om vi har permission for å få tak i FINE_LOCATION (accurate location).
    public static boolean checkPermission(Context context) {
        boolean permissionGranted = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "checkPermission() result: "+permissionGranted);
        return permissionGranted;
    }

    //spør om permission viss det ikke er gitt.
    public static void askPermission(Activity activity) {
        Log.d(TAG, "askPermission()");
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQ_PERMISSION
        );
    }

}
