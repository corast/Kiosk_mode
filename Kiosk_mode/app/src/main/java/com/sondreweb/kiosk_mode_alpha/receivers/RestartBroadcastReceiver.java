package com.sondreweb.kiosk_mode_alpha.receivers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.widget.Toast;

import com.sondreweb.kiosk_mode_alpha.utils.AppUtils;
import com.sondreweb.kiosk_mode_alpha.services.GeofenceTransitionService;
import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;

/**
 * Created by sondre on 27-Jan-17.
 * WakefulBroadcastReciever slik at telefonen ikke kan gå i sleepmode før denne er ferdig å kjøre.
 */

public class RestartBroadcastReceiver extends WakefulBroadcastReceiver {

    private static final String ACTION_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN";

    //

    private final static String TAG = RestartBroadcastReceiver.class.getSimpleName();

    //denne lytter kunn til com.sondreweb.GeoFencingAlpha.Activity.RestartGeofencing.
    @Override
    public void onReceive(Context context, Intent intent) {
        //dette er bare tull altså
        String action = intent.getAction();
        Log.d(TAG,"¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤");
        Log.d(TAG,action);
        Log.d(TAG,"¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤");
        switch(action){
            case Intent.ACTION_BOOT_COMPLETED:
                //TODO: re-registrer Geofencet igjenn.
                //sendToast(context);
                //checkIfGeofenceIsAlive(context);

                checkIfServiceRunning(context);
                sendToast(context);
                break;
            case ACTION_SHUTDOWN:
                if(PreferenceUtils.isKioskModeActivated(context)){
                    /*
                    *   Viss vi havner her, betyr det av enheten skrus av, selv i Kiosk mode. Ikke bra.
                    * */

                    //TODO: sende en respons på dette over mobilnett, gitt at dette går kjapt nok
                    //  dersom vi har oppretholdt forbildensen hele tiden kanskje.
                }
                /*
                *   Når vi skal skru av telefonen havner vi her.
                * */
                break;

            default:
                Log.d(TAG,"Action som ble motatt i BroadcastReceiver:"+ action);
        }
        // context.startService(new Intent(context, GeofenceTransitionService.class));
    }

    public void sendToast(Context context){
        Toast toast = Toast.makeText(context, "OnBootComplete",Toast.LENGTH_LONG);
        toast.show();
    }

    public void checkIfGeofenceIsAlive(Context context){
        if(! AppUtils.isServiceRunning(GeofenceTransitionService.class, context)){ //false betyr at servicen ikke kjørere.
            if(PreferenceUtils.isKioskModeActivated(context)) { //Dersom Kiosk Mode er activatet, så kan vi også starte Servicen vår.
                //dersom servicen vår ikke kjører, så starter vi den.
                //TODO Legg til actions viss det trengs å vite hvor servicen startet fra.
                Intent GeofenceTransitionServiceIntent = new Intent(context, GeofenceTransitionService.class);
                GeofenceTransitionServiceIntent.setAction(GeofenceTransitionService.RESTART_GEOFENCES); //slik at vi vet at det mobilen har blir resatt, vi må då muligens gå direkte til MonumentVandring.
                context.startService(GeofenceTransitionServiceIntent);
            }
        }
    }

    public void checkIfServiceRunning(Context context){
        if(! AppUtils.isServiceRunning(GeofenceTransitionService.class, context)){
            if(AppUtils.isGooglePlayServicesAvailable(context)){
                //Servicen kjører ikke, men googlePlayService er tilgjengelig.
                Intent GeofenceTransitionServiceIntent = new Intent(context, GeofenceTransitionService.class);
                GeofenceTransitionServiceIntent.setAction(GeofenceTransitionService.RESTART_GEOFENCES); //slik at vi vet at det mobilen har blir resatt, vi må då muligens gå direkte til MonumentVandring.
                context.startService(GeofenceTransitionServiceIntent);
            }
            else
            {
                Intent GeofenceTransitionServiceIntent = new Intent(context, GeofenceTransitionService.class);
                GeofenceTransitionServiceIntent.setAction(GeofenceTransitionService.START_SERVICE); //slik at vi vet at det mobilen har blir resatt, vi må då muligens gå direkte til MonumentVandring.
                context.startService(GeofenceTransitionServiceIntent);
            }
        }
    }

    public void reRegisterGeofence(){
        //TODO: reRegisterGeofence når vi skrur på mobilen igjenn.
    }

}
