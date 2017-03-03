package com.sondreweb.kiosk_mode_alpha.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by sondre on 23-Feb-17.
 */

public class TestAccessiblityService extends AccessibilityService {

    //WHITELISTED APPS:
    protected final String GeofencingApp = "com.sondreweb.geofencingAlpha";
    protected final String LauncherApp = "com.sondreweb.kiosk_mode_alpha";

    //BLACK LIST
    protected final String Settings = "com.android.settings";

    //WHITELIST LIST
    ArrayList<String> WhiteList = new ArrayList<String>(Arrays.asList(GeofencingApp, LauncherApp)); //populate med appene vi tilater i kiosk mode.

    private static final String TAG = TestAccessiblityService.class.getSimpleName();

    private ActivityManager activityManager;

    //private ActivityManager activityManager;

    @Override
    protected void onServiceConnected() {
        Log.d(TAG,TAG+" started");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        initiateWhitelist();
            //Vi er på utkikk etter alle eventer som har med å forandre Window state
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
            //må lese meg opp på denne
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;

        info.flags = AccessibilityServiceInfo.DEFAULT;
            //slik at det er en tidsbegrensning på hvor lenge vi er connectet til en event(tror jeg).
        info.notificationTimeout = 100;

        setServiceInfo(info);
        //super.onServiceConnected();
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if( event != null && event.getPackageName()!= null ){

        //TODO check Whitelist
        if(checkIfWhiteListed(event.getPackageName())){
            Log.d(TAG,"Denne appen er grei");
        }else {
            if(PreferenceUtils.isKioskModeActivated(this)){
                Toast.makeText(this.getApplicationContext(), "Ikke monumentVandring", Toast.LENGTH_SHORT).show();



                if(event.getPackageName().toString().equalsIgnoreCase(Settings)){
                    //Log.d(TAG,"ActivityManager "+getActivityManager().toString());
                    //getActivityManager().killBackgroundProcesses(event.getPackageName().toString());
                    //getActivityManager().killBackgroundProcesses(event.getPackageName().toString());
                    //HomeActivity.KillProcess(event.getPackageName().toString());
                    //TODO: Finn ut hvordan vi forhinder noen aktiviter å starte. Muligens vi bare kontrollerer menuen med passord innlogging for å starte blackListed Aplications.

                    }
                }
            }
        }
    }

    public void initiateWhitelist(){
        WhiteList.add("com.sondreweb.geofencingAlpha");
        WhiteList.add("com.sondreweb.kiosk_mode_alpha");
    }

    @Override
    public void onInterrupt() {

    }

    public void WhiteList(){
        String WhiteList = "com.sondreweb.geofencingApha";
    }

    //TODO tenk på hvor mange vi faktisk trenger etter hvert.
    public boolean checkIfWhiteListed(CharSequence packageName){
        for( String pack : WhiteList){
            Log.d(TAG,pack+ " == "+packageName +" bool: "+ pack.equals(packageName));
            if(packageName.toString().equalsIgnoreCase(pack)) {
                return true;
            }
        }
        return false;
    }

    public ActivityManager getActivityManager(){
        //activityManager = (ActivityManager) context.getSystemService(this.getApplicationContext());
            if(activityManager != null) {
                try {
                    activityManager = (ActivityManager) this.getApplication().getSystemService(Context.ACTIVITY_SERVICE);
                    return activityManager;
                }catch (ClassCastException e){
                    Log.e(TAG,e.getMessage());
                    return (ActivityManager) this.getApplication().getSystemService(ACTIVITY_SERVICE);
                }
            }else
                return activityManager;
    }

}
