package com.sondreweb.kiosk_mode_alpha.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by sondre on 24-Feb-17.
 * Ansvar for å holde oversikt over vi faktisk skal være i MonumentVandrings modus eller ikke, samt å lagre
 * om vi skal eller ikke.
 */

public class PreferenceUtils {

    private static final String TAG = PreferenceUtils.class.getSimpleName();
    //KEY som brukes for å hente ut en verdi fra Preferancene i systemet. Denne er viktig kun denne appen har tilgang til.
    private static final String PREF_KIOSK_MODE = "pref_kiosk_mode";
    private static final String PREF_ADMIN_DEVICE = "pref_admin_device";
    private static final String PREF_KIOSK_APP = "pref_kiosk_application";

    private static final String DEFAULT_APP = "com.android.chrome"; //VI setter Chrome nå i starten for å teste.

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
            Log.d(TAG, "Successfully set Kiosk mode to: "+active);
        }else
            Log.e(TAG,"Error setting Kiosk mode to: "+active);
    }

    //For å sjekke om vi har DeviceAdmin rettigheter eller ikke.
    public static boolean isAppDeviceAdmin(final Context context){
        Log.d(TAG,"isAppDeviceAdmin :");
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
        if(sharedPreferences.edit().putString(PREF_KIOSK_APP, appPackage).commit()){

        }
    }

    public static String getPrefkioskModeApp(final Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        //TODO: check if package is installed
        String kiosk_app = sharedPreferences.getString(PREF_KIOSK_APP, DEFAULT_APP);
        /*
        *   En kjapp test på om Packagen som vi faktisk har er installert på enheten.
        * */
        if(AppUtils.isPacketInstalled(kiosk_app,context)){//True dersom den eksiterer
            return kiosk_app;
        }
        return DEFAULT_APP; //Da bare returnere vi Default app stringen, siden den vet vi eksiterer.
    }
}