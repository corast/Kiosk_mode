package com.sondreweb.kiosk_mode_alpha;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by sondre on 24-Feb-17.
 * Ansvar for å holde oversikt over vi faktisk skal våre i MonumentVandrings modus eller ikke, samt å sette
 * at vi skal være det.
 */

public class PreferenceUtils {


    private static final String TAG = PreferenceUtils.class.getSimpleName();
    //KEY som brukes for å hente ut en verdi fra Preferancene i systemet. Denne er viktig kun denne appen har tilgang til.
    private static final String PREF_KIOSK_MODE = "pref_kiosk_mode";
    private static final String PREF_ADMIN_DEVICE = "pref_admin_device";

    public static boolean isKioskModeActivated(final Context context){
        Log.d(TAG,"isKioskModeActivated ctx:"+context.toString());
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(PREF_KIOSK_MODE,false); //default value er false
        /*Default value er false, siden dersom vi ikke har registrert Kiosk mode enda, så er lurt å ikke gå inn i Kiosk heller
        *   Dette må ses på senere. Dersom vi kjører en factory reset, så mister vi alt uansett, Ikke noe vi får gjordt der ifra.
        * */
    }



    //TODO:finn ut hvem som kan faktisk forandre på denne.
    public static void setKioskModeActive(final boolean active, final Context context){
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
}
