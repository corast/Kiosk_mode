package com.sondreweb.kiosk_mode_alpha.services;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.IntDef;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by sondre on 23-Feb-17.
 */

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {

    //WHITELISTED PACKAGENAMES:
    protected final String GeofencingApp = "com.sondreweb.geofencingAlpha";
    //Kan ikke whiteliste denne, siden det er mulighet for å gå tilbake her.
    protected final String LauncherApp = "com.sondreweb.kiosk_mode_alpha";
    //Keyboard kan være problematisk, litt for vag i noen tilfeller.
    protected final String Keyboard = "com.google.android.inputmethod.latin";

    //WHITELISTED CLASSNAMES:
    protected final String LogInnAdmin = "class com.sondreweb.kiosk_mode_alpha.activities.LoginAdminActivity";

    //BLACKLISTED PACKAGENAMES:
    /* Settings packagen, selvforklarende */
    protected final String Settings = "com.android.settings";
    /*
    * Dette er packagen som har med hvordan vi velger å starte en prosess som systemet ikke har en valg måte å håndtere,
    * som åpning av en fil hvor vi velger hvilken applikasjon som skal gåndtere dette
    * */
    protected final String ResolverActivity = "com.android.internal.app.ResolverActivity";
    /* Launcheren fra Systemet, som følger med.*/
    protected final String DefaultLauncher = "com.android.launcher";

    //WHITELISTED PACKAGENAMES LIST:
    ArrayList<String> WhiteListPackages = new ArrayList<String>(
            Arrays.asList(
                    GeofencingApp,
                    Keyboard,
                    LauncherApp
            )); //populate med appene vi tillater i kiosk mode.

    //WHITELISTED PACKAGENAMES LIST:
    ArrayList<String> WhiteListedClasses = new ArrayList<>(
           Arrays.asList(
                   LogInnAdmin
           ));
    private static final String TAG = AccessibilityService.class.getSimpleName();

    private ActivityManager activityManager;

    //private ActivityManager activityManager;

    /*
    * onServiceConnected: når vi starter opp Servicen.
    * Setter Event typen den lytter etter å være WINDOW_STATE_CHANGED.
    * Setter feedbackType til å være FEEDBACK_ALL_MASK;
    * */
    @Override
    protected void onServiceConnected() {
        Log.d(TAG,TAG+" started  €€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        //initiateWhitelist();
            //Vi er på utkikk etter alle eventer som har med å forandre Window state
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

            //Gir oss tillatese på å gi feedback på alle former, Spoken, Haptic, Audible, Visual, Generic og Braille.
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
            //Setter hvilket flag vi skal bruke: Default service is invoked only if no package specific one exists.
            // In case of more than one package specific service only the earlier registered is notified.
        info.flags = AccessibilityServiceInfo.DEFAULT;
            //The timeout after the most recent event of a given type before an AccessibilityService is notified.
            //Delay før vi får inn Eventet som har skjedd.
        info.notificationTimeout = 10; //0.1 sekunder er nok for bruken til å se hva som har skjedd, men ikke nok tid til å faktisk gjøre noe.

        setServiceInfo(info);

        //super.onServiceConnected();

        WhiteListPackages.add(PreferenceUtils.getPrefkioskModeApp(getApplicationContext()));
    }

        //TODO: fiks google søk

    //TODO: Fiks på Whitelisten, og kanskje noe blacklisting, slik at vi er sikker på at alt uønsket ikke kan framkomme underveis, samtidig så er Packagename litt universel for flere Classnames.
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

            //Enkel sjekk på at det er WindowStateChange event og at event faktisk ikke er null, kanskje litt overflødig med å sjekke alt dette.
        if( event != null && event.getPackageName()!= null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ){

            if(PreferenceUtils.isKioskModeActivated(getApplicationContext())){
                Log.d(TAG,"---------------------------------------------");
                Log.d(TAG,"Kiosk mode: True");
                Log.d(TAG,"event: "+event.toString());
                Log.d(TAG,"Event classname: "+event.getClassName());
                Log.d(TAG,"Event packagename: "+event.getPackageName());

                checkIfOkayWindowState(event);
            }else {
                Log.d(TAG,"---------------------------------------------");
                Log.d(TAG,"Event triggered, men KioskMode er Off/False !!!!!!!!!!!!!!!!!!!!!!");
                Log.d(TAG,"event: "+event.toString());
                Log.d(TAG,"Event classname: "+event.getClassName());
                Log.d(TAG,"Event packagename: "+event.getPackageName());
                checkIfOkayWindowState(event);
            }
        }
    }

    /*
    *   Denne skal fungere på denne måten:
    *       Dersom appen er godkjent så skal vi ikke gjøre noe.
    *           Så vi må sjekke om packageName er godkjent og at classname er godkjent på visse apper.
    *       Dersom appen ikke er godkjent
    *           Så må vi sjekke om det er spesial tilfeller hvor vi må gå Home istedetfor Back.
    * */

    public void checkIfOkayWindowState(AccessibilityEvent event){

        if(event.getPackageName().equals(PreferenceUtils.getPrefkioskModeApp(getApplicationContext()))){ //sjekker at vi er innefor Packages som er godkjent, som er MonumentVandring elns.
            Log.d(TAG,"Kiosk mode appen er grei");
        }
        else if(checkIfWhiteListedPackage(event.getPackageName())) {
            Log.d(TAG,"Denne Appen er grei"); //TODO: finn ut om packagename gjelder for hele appen min, og ikke hver aktivitet.
        }else if(checkIfWhiteListedClass(event.getClassName())){
            Log.d(TAG,"Denne Appen er grei"); //TODO: finn ut om packagename gjelder for hele appen min, og ikke hver aktivitet.
        }
        else if(event.getClassName().equals("android.widget.FrameLayout") && event.getPackageName().equals("com.android.systemui")){
            Log.d(TAG,"Denne Appen er grei");
        }else{
            if(PreferenceUtils.isKioskModeActivated(getApplicationContext())){
                //Må gjøre noe med vinduet, 2 spesial tilfeller.
                if(event.getClassName().equals(ResolverActivity)){
                    this.performGlobalAction(GLOBAL_ACTION_HOME);
                }else if(event.getPackageName().equals(DefaultLauncher)){
                    this.performGlobalAction(GLOBAL_ACTION_HOME);
                }else{
                    Log.d(TAG,"Global_action_back <<<<<<<<<<<<<<<<<<<<");
                    this.performGlobalAction(GLOBAL_ACTION_HOME);
                }
                Toast.makeText(this.getApplicationContext(), "Venligst ikke bytt Applikasjon", Toast.LENGTH_SHORT).show();
            }

            Log.d(TAG,"denne Appen er Ikke grei");
        }
        Log.d(TAG,"---------------------------------------------");
    }


    public void checkIfOkState(AccessibilityEvent event){
        if(event.getPackageName().equals(PreferenceUtils.getPrefkioskModeApp(getApplicationContext()))){ //sjekker at vi er innefor Packages som er godkjent, som er MonumentVandring elns.
            Log.d(TAG,"Kiosk mode appen er grei");
        }
        else if(checkIfWhiteListedPackage(event.getPackageName())) {
            Log.d(TAG,"Denne Appen er grei"); //TODO: finn ut om packagename gjelder for hele appen min, og ikke hver aktivitet.
        }else if(event.getClassName().equals("android.widget.FrameLayout") && event.getPackageName().equals("com.android.systemui")){
            Log.d(TAG,"Denne Appen er grei");
        }else{
            Log.d(TAG,"Denne Appen er Ikke grei");

        }
        Log.d(TAG,"---------------------------------------------");
    }


    public void gammeKodeTesting(AccessibilityEvent event){
        if(event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED){
            Log.d(TAG,"Event: "+ event.toString());
        }
        //TODO check Whitelist
        if(checkIfWhiteListedPackage(event.getPackageName())){
            Log.d(TAG,"Denne appen er grei");
        }else if(checkIfWhiteListedClass(event.getClassName())){

        }else {
            if(PreferenceUtils.isKioskModeActivated(this)){
                Toast.makeText(this.getApplicationContext(), "Ikke Monument Vandring", Toast.LENGTH_SHORT).show();

                if(event.getPackageName().toString().equalsIgnoreCase(Settings)){
                    //Log.d(TAG,"ActivityManager "+getActivityManager().toString());
                    //getActivityManager().killBackgroundProcesses(event.getPackageName().toString());
                    //getActivityManager().killBackgroundProcesses(event.getPackageName().toString());
                    //HomeActivity.KillProcess(event.getPackageName().toString());
                    //TODO: Finn ut hvordan vi forhinder noen aktiviter å starte. Muligens vi bare kontrollerer menuen med passord innlogging for å starte blackListed Aplications.
                    Log.d(TAG, "stop Settings test");
                    //Intent stopIntent = new Intent("com.android.settings");
                    //stopIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    //stopIntent.setFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                    //startActivity(stopIntent);
                    //(stopIntent);
                    Intent app = new Intent("com.sondreweb.kiosk_mode_alpha.activities.MapsActivity");
                    //startActivity(app);
                    WindowManager windowManager;
                    this.performGlobalAction(GLOBAL_ACTION_BACK);
                    //1this.performGlobalAction(GLOBAL_ACTION_HOME);

                }
                else if(event.getClassName().equals("com.android.systemui.recent.RecentsActivity")){
                    /*
                    *   Siden vi ikke tillater Recent button å bli clikket, må vi disable denne "appen".
                    *   Dersom vi går GLOBAL_ACTION_BACK her så havner vi i default launcheren, og ut av vår egen.
                    *   Må dermed passe på at denne går tilbake til HOME.
                    * */
                    this.performGlobalAction(GLOBAL_ACTION_HOME);
                }
                else{
                    Log.d(TAG, "Ikke en godkjent event:");


                    if(event.getPackageName().toString().equalsIgnoreCase("com.android.keyguard")){
                        Log.d(TAG,"com.android.keyguard prøver å gjøre noe");
                        this.performGlobalAction(GLOBAL_ACTION_BACK);
                    }else if(event.getPackageName().toString().equalsIgnoreCase("android")){ //tar alt fra systemet.
                        Log.d(TAG,"android prøver å gjøre noe");
                        this.performGlobalAction(GLOBAL_ACTION_BACK);
                    }else if(event.getClassName().equals("com.android.systemui.recent.RecentsActivity")){
                        this.performGlobalAction(GLOBAL_ACTION_HOME);
                    }else if(event.getClassName().equals("android.widget.FrameLayout") && event.getPackageName().equals("com.android.keyguard")){
                        this.performGlobalAction(GLOBAL_ACTION_BACK);
                    }else if(event.getClassName().equals("com.android.launcher")){
                        this.performGlobalAction(GLOBAL_ACTION_HOME);
                    }
                            /*event.getPackageName().equals("com.android.systemui"*/
                }
            }
        }
    }

    public void checkIfOkayWindowStateTest(AccessibilityEvent event){
        if(event.getPackageName().equals(PreferenceUtils.getPrefkioskModeApp(getApplicationContext()))){
            Log.d(TAG,"Denne appen er grei");
        }
        else if(checkIfWhiteListedPackage(event.getPackageName())) {
            Log.d(TAG,"Denne Appen er grei"); //TODO: finn ut om packagename gjelder for hele appen min, og ikke hver aktivitet.
        }else{
            if(PreferenceUtils.isKioskModeActivated(getApplicationContext())){
                //Må gjøre noe med vinduet, 2 spesial tilfeller.
                if(event.getClassName().equals("com.android.systemui.recent.RecentsActivity")){
                    this.performGlobalAction(GLOBAL_ACTION_HOME);
                }else if(event.getPackageName().equals("com.android.launcher")){
                    this.performGlobalAction(GLOBAL_ACTION_HOME);
                }else{
                    //Noe galt her

                    this.performGlobalAction(GLOBAL_ACTION_BACK);
                }
            }
        }
    }


    /*      GAMMEL KODE
    *       /*
                if(event.getPackageName().toString().equalsIgnoreCase(Settings)){
                    //Log.d(TAG,"ActivityManager "+getActivityManager().toString());
                    //getActivityManager().killBackgroundProcesses(event.getPackageName().toString());
                    //getActivityManager().killBackgroundProcesses(event.getPackageName().toString());
                    //HomeActivity.KillProcess(event.getPackageName().toString());
                    //TODO: Finn ut hvordan vi forhinder noen aktiviter å starte. Muligens vi bare kontrollerer menuen med passord innlogging for å starte blackListed Aplications.
                    Log.d(TAG, "stop Settings test");
                    //Intent stopIntent = new Intent("com.android.settings");
                    //stopIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    //stopIntent.setFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                    //startActivity(stopIntent);
                    //(stopIntent);
                    Intent app = new Intent("com.sondreweb.kiosk_mode_alpha.activities.MapsActivity");
                    //startActivity(app);
                    WindowManager windowManager;
                    this.performGlobalAction(GLOBAL_ACTION_BACK);
                    //1this.performGlobalAction(GLOBAL_ACTION_HOME);

                    }
                    else if(event.getClassName().equals("com.android.systemui.recent.RecentsActivity")){
                    */

                    /*
                    *   Siden vi ikke tillater Recent button å bli clikket, må vi disable denne "appen".
                    *   Dersom vi går GLOBAL_ACTION_BACK her så havner vi i default launcheren, og ut av vår egen.
                    *   Må dermed passe på at denne går tilbake til HOME.
                    * */

                    /*
                        this.performGlobalAction(GLOBAL_ACTION_HOME);
                    }
                    else{
                            Log.d(TAG, "Ikke en godkjent event:");


                            if(event.getPackageName().toString().equalsIgnoreCase("com.android.keyguard")){
                                Log.d(TAG,"com.android.keyguard prøver å gjøre noe");
                                this.performGlobalAction(GLOBAL_ACTION_BACK);
                            }else if(event.getPackageName().toString().equalsIgnoreCase("android")){ //tar alt fra systemet.
                                Log.d(TAG,"android prøver å gjøre noe");
                                this.performGlobalAction(GLOBAL_ACTION_BACK);
                            }else if(event.getClassName().equals("android.widget.FrameLayout") && event.getPackageName().equals("com.android.keyguard")){
                                this.performGlobalAction(GLOBAL_ACTION_BACK);
                            }else if(event.getClassName().equals("com.android.launcher")){
                                this.performGlobalAction(GLOBAL_ACTION_HOME);
                            }

                        }
                    } */


    @Override
    public void onInterrupt() {}


    public boolean checkIfWhiteListedPackage(CharSequence packageName){
        for( String packageToCheck : WhiteListPackages){
            //Log.d(TAG,pack+ " == "+packageName +" bool: "+ pack.equals(packageName));
            if(packageName.toString().equalsIgnoreCase(packageToCheck)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkIfWhiteListedClass(CharSequence className){
        for ( String classToCheck : WhiteListedClasses){
            if(className.toString().equalsIgnoreCase(classToCheck)){
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
