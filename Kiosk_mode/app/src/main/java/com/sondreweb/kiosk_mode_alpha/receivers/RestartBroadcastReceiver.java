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
 *  Ansvar for å starte servicen ved oppstart av enheten(BOOT_COMPLETE).
 */

public class RestartBroadcastReceiver extends WakefulBroadcastReceiver {

    //System broadcast for SHUTDOWN.
    private static final String ACTION_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN";

    private final static String TAG = RestartBroadcastReceiver.class.getSimpleName();

    /*
    *   Denne lytter til: com.sondreweb.GeoFencingAlpha.Activity.RestartGeofencing.
    *                     Intent.ACTION_BOOT_COMPLETED og android.intent.action.ACTION_SHUTDOWN
    *
    * */
    @Override
    public void onReceive(Context context, Intent intent) {
        //dette er bare tull altså
        String action = intent.getAction();
        switch(action){
            case Intent.ACTION_BOOT_COMPLETED:
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
                *   Når vi skal skru av telefonen på normalt vis havner vi her.
                * */
                break;

            default:
                Log.d(TAG,"Action som ble mottatt i BroadcastReceiver:"+ action);
        }
    }

    //Debug funskjon for å sende Toasts.
    public void sendToast(Context context){
        Toast toast = Toast.makeText(context, "OnBootComplete",Toast.LENGTH_LONG);
        toast.show();
    }


    //Starter servicen i bakgrunn med geofence dersom nødvendig.
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

}
