package com.sondreweb.kiosk_mode_alpha.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Calendar;
import java.util.TimeZone;

import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.services.GeofenceTransitionService;

/**
 * Created by sondre on 24-Feb-17.
 * Ansvar for å holde oversikt over vi faktisk skal være i MonumentVandrings modus eller ikke, samt å lagre
 * om vi skal eller ikke.
 */

public class PreferenceUtils {

    private static final String TAG = PreferenceUtils.class.getSimpleName();
    //KEY som brukes for å hente ut en verdi fra Preferancene i systemet. Denne er viktig kun denne appen har tilgang til.

    public static final String PREF_KIOSK_MODE = "pref_kiosk_mode";

    private static final String PREF_ADMIN_DEVICE = "pref_admin_device";

    private static final String PREF_GEOFENCE_UPDATE_INTERVAL = "pref_kiosk_geofence_update_interval";
    private static final String PREF_GEOFENCE_FASTEST_UPDATE_INTERVAL = "pref_kiosk_geofence_fastest_update_interval";

    public static final int ONE_MINUTE_IN_MILLIS = 1000*60;
    public static final int ONE_SECOND_IN_MILLIS = 1000;
    public static final int THIRTY_SECONDS_IN_MILLIS = ONE_SECOND_IN_MILLIS *30;

    private static final int DEFAULT_PREF_GEOFENCE_UPDATE_INTERVAL = ONE_SECOND_IN_MILLIS; //2 minutter
    public static final int DEFAULT_GEOFENCE_FASTEST_UPDATE_INTERVAL = 10*ONE_SECOND_IN_MILLIS; //10 sekunder

    /*
    *   Her bør MonumentVandring Appen stå.
    * */

    private static final String PREF_KIOSK_APP_KEY = "pref_kiosk_app"; //Dette er Appen som vi skal starte opp.

    //kan være hva som helst
    //TODO: si ifra om at Kiosk Mode app ikke er satt.
    public static final String DEFAULT_APP = "no.app.found";
            //"com.android.chrome"; //VI setter Chrome nå i starten for å teste.

    //private static final String DEFAULT_APP_TEST = "com.Company.Monumentvandring"; //VI setter Chrome nå i starten for å teste.


    //private static final String DEFAULT_APP = "com.Company.Monumentvandring"; //VI setter MonumentVandring nå i starten for å teste.
    //private static com.android.calculator2


    public static boolean isKioskModeActivated(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_KIOSK_MODE,false); //default value er false
        /*Default value er false, siden dersom vi ikke har registrert Kiosk mode enda, så er lurt å ikke gå inn i Kiosk heller
        *   Dette må ses på senere. Dersom vi kjører en factory reset, så mister vi alt uansett, Ikke noe vi får gjordt der ifra.
        * */
    }

    //TODO:finn ut hvem som kan faktisk forandre på denne.
    public static void setKioskModeActive( final Context context, boolean active){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        //Henter Preferansene til systemet.

        if(sharedPreferences.edit().putBoolean(PREF_KIOSK_MODE, active).commit()){
            if(active){
                //TODO: be servicen starte og hente lokasjon.
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
                Intent startGeofenceIntent = new Intent(context,GeofenceTransitionService.class);
                startGeofenceIntent.setAction(GeofenceTransitionService.START_GEOFENCE);
                //localBroadcastManager.sendBroadcast(startGeofenceIntent);
            }
            Log.d(TAG, "Successfully set Kiosk mode to: "+active);
        }else
            Log.e(TAG,"Error setting Kiosk mode to: "+active);
    }

    //For å sjekke om vi har DeviceAdmin rettigheter eller ikke.
    public static boolean isAppDeviceAdmin(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_ADMIN_DEVICE,false);
        //Default value er False, siden det betyr at DEVICE amin ikke er satt uansett.
    }

    //For å sette om vi har DeviceAdmin rettigheter eller ikke.
    public static void setPrefAdminDevice(final boolean active, final Context context){
        //sharedPreferences er et Singleton object, så vi får bare tilbake siste isntance av denne.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        //Henter Preferansene til systemet.
        if(sharedPreferences.edit().putBoolean(PREF_ADMIN_DEVICE, active).commit()){
            Log.d(TAG, "Successfully set Admin to: "+active);
        }else
            Log.e(TAG,"Error setting Admin to: "+active);
    }

    public static void setPrefkioskModeApp(final String appPackage,final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putString(PREF_KIOSK_APP_KEY, appPackage).apply();
    }

    public static String getPrefkioskModeApp(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        //TODO: check if package is installed
        String kiosk_app = sharedPreferences.getString(PREF_KIOSK_APP_KEY, DEFAULT_APP);
        /*
        *   En kjapp test på om Packagen som vi faktisk har er installert på enheten.
        * */
        //Log.d(TAG,"isPacketInnstalled : "+kiosk_app +" -> "+ AppUtils.isPacketInstalled(kiosk_app,context));
        if(AppUtils.isPacketInstalled(kiosk_app,context)){//True dersom den eksitere
            return kiosk_app;
        }
        return DEFAULT_APP; //Da bare returnere vi Default app stringen, siden den vet vi eksiterer.
    }

    /*
    *   lagrer unna når vi synchroniserer data.
    * */
    public static final String SYNCHRONIZATION_STATISTICS_KEY = "last_synchronize";
    private static final String SYNCHRONIZATION_TIME_DEFAULT = "Never";
    public static void setSynchronizationTime(final Context context, String time){
        //henter datoen nå og lagrer denne unna.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if(time == null){
            TimeZone tzNorway = TimeZone.getTimeZone("GMT+2");
            Calendar c = Calendar.getInstance(tzNorway);
            //Gir oss tid på formatet dag/månde/år time:minutt:sekund
            String timeC = c.get(Calendar.DAY_OF_MONTH) + "/" + c.get(Calendar.MONTH) +
                    "/" + c.get(Calendar.YEAR) + " " + c.get(Calendar.HOUR_OF_DAY) +
                    ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND);
            sharedPreferences.edit().putString(SYNCHRONIZATION_STATISTICS_KEY,timeC).apply();
        }else
        {
            sharedPreferences.edit().putString(SYNCHRONIZATION_STATISTICS_KEY,time).apply();
        }
    }

    public static String getTimeSinceLastSynchronization(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(SYNCHRONIZATION_STATISTICS_KEY,SYNCHRONIZATION_TIME_DEFAULT);
    }

    public static final String SYNCHRONIZE_GEOFENCE_KEY = "pref_synchronize_geofence";
    private static final String SYNCHRONIZE_GEOFENCE_DEFAULT = "Never";

    public static String getPrefLastSynchroizeGeofence(final Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(SYNCHRONIZE_GEOFENCE_KEY, SYNCHRONIZE_GEOFENCE_DEFAULT);
    }

    public static void updatePrefLastSynchronizeGeofence(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        //Get time now.
        TimeZone tzNorway = TimeZone.getTimeZone("GMT+2");
        Calendar c = Calendar.getInstance(tzNorway);
        //Gir oss tid på formatet dag/månde/år time:minutt:sekund
        String time = c.get(Calendar.DAY_OF_MONTH) + "/" + c.get(Calendar.MONTH) +
                "/" + c.get(Calendar.YEAR) + " " + c.get(Calendar.HOUR_OF_DAY) +
                ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND);
        sharedPreferences.edit().putString(SYNCHRONIZE_GEOFENCE_KEY,time).apply();
    }

/*
*   Metoder for å hente ut og sette oppdatersings tid på LocationServices.FusedLocationApi LocationRequests.
* */

    public static void setPrefGeofenceUpdateInterval(int UPDATE_INTERVAL, final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putInt(PREF_GEOFENCE_UPDATE_INTERVAL, UPDATE_INTERVAL).apply();
    }

    public static int getPrefGeofenceUpdateInterval(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        //Henter ut Int verdien til update_interval.
        return sharedPreferences.getInt(PREF_GEOFENCE_FASTEST_UPDATE_INTERVAL,DEFAULT_PREF_GEOFENCE_UPDATE_INTERVAL );
    }

    public static void setPrefGeofenceFastestUpdateInterval(int FASTEST_UPDATE_INTERVAL, final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putInt(PREF_GEOFENCE_FASTEST_UPDATE_INTERVAL, FASTEST_UPDATE_INTERVAL).apply();
    }

    public static int getPrefGeofenceFastestUpdateInterval(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(PREF_GEOFENCE_FASTEST_UPDATE_INTERVAL,DEFAULT_GEOFENCE_FASTEST_UPDATE_INTERVAL);
    }

    private static final boolean defaultSync = false;
    public static boolean getSynchronizeAutomatically(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean( context.getResources().getString(R.string.KEY_DATA_SYNCHRONIZE_AUTOMATIC), false);
    }

    private static final boolean defaultVibrate = false;
    public static boolean getVibrateSettings(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(context.getResources().getString(R.string.KEY_NOTIFICATIONS_VIBRATE_OUTISDE_GEOFENCE),defaultVibrate);
    }

    private static final String defaultVibrateTime = "500";
    public static int getVibrateTimeSettings(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String value = sharedPreferences.getString(context.getString(R.string.KEY_NOTIFICATIONS_VIBRATE_TIME),defaultVibrateTime);
                //Vi vet at stringen er på int format uansett. 500, 1000 etc.
        return Integer.parseInt(value);
    }

    public static boolean getPrefOverlayOn(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(context.getString(R.string.KEY_SECURITY_GEOFENCE_OVERLAY), false);
    }

    public static int getGeofenceUpdateInterval(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String value = sharedPreferences.getString(context.getString(R.string.KEY_GEOFENCE_UPDATEINTERVAL),context.getString(R.string.DEFAULT_GEOFENCE_UPDATEINTERVAL));
        return Integer.parseInt(value);
    }

    public static int getOutsideGeofenceUpdateInterval(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String value = sharedPreferences.getString(context.getString(R.string.KEY_OUTSIDE_GEOFENCE_UPDATE_INTERVAL),context.getString(R.string.DEFAULT_GEOFENCE_UPDATEINTERVAL));
        return Integer.parseInt(value);
    }

    public static String getOutsideGeofenceUpdateIntervalAsString(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String value = sharedPreferences.getString(context.getString(R.string.KEY_OUTSIDE_GEOFENCE_UPDATE_INTERVAL),context.getString(R.string.DEFAULT_GEOFENCE_UPDATEINTERVAL));
        int iValue = Integer.parseInt(value) / 1000;
        int min = iValue / 60;
        int sec = iValue % 60;
        if(min == 0){
            return sec+" sec";
        }
        return min + "min "+ sec +" sec";
    }

    public static final String no_url = "No URL found";
    public static String getSynchGeofenceUrl(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(context.getString(R.string.KEY_URL_SERVER_GEOFENCE),no_url);
    }

    public static String getSynchStatisticsUrl(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(context.getString(R.string.KEY_URL_SERVER_STATISTICS),no_url);
    }

}
