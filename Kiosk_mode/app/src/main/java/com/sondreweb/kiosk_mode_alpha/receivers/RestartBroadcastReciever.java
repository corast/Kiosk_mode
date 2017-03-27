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

public class RestartBroadcastReciever extends WakefulBroadcastReceiver {

    private final static String TAG = RestartBroadcastReciever.class.getSimpleName();

    /*Broadcast Cases som vi bruker*/
    private final static String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    private final static String SCREEN_OFF = "android.intent.action.BOOT_COMPLETED";

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
                checkIfGeofenceIsAlive(context);
                sendToast(context);
                break;
            case Intent.ACTION_SCREEN_OFF:
                Log.d(TAG, "ACTION SCREEN OFF");
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
                Intent GeofenceTransitionService = new Intent(context, GeofenceTransitionService.class);
                GeofenceTransitionService.setAction("RESTART"); //slik at vi vet at det mobilen har blir resatt, vi må då muligens gå direkte til MonumentVandring.
                context.startService(GeofenceTransitionService);
            }
        }
    }

    public void reRegisterGeofence(){
        //TODO: reRegisterGeofence når vi skrur på mobilen igjenn.
    }

}
