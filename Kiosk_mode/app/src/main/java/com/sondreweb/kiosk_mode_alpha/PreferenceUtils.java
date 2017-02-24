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
        sharedPreferences.edit().putBoolean(PREF_KIOSK_MODE, active).apply();
    }
}
