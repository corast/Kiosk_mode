package com.sondreweb.kiosk_mode_alpha;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * Created by sondre on 16-Feb-17.
 */
public class HomeActivity extends FragmentActivity {


    public final static String TAG = HomeActivity.class.getSimpleName();
    private final static String APP = "com.sondreweb.geofencingalpha";


    private PackageManager packageManager;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        context = this;
        startAccessibilityService();
        //checkIfAppInstalled(this, "testing");
    }

    public void startAccessibilityService(){
        Log.d(TAG,"startAccessibilityService");
        Intent accessibilityServiceIntent = new Intent(this,TestAccessiblityService.class);
        startService(accessibilityServiceIntent);
    }

    public void showApps(View v) {

        if(checkIfAppInstalled(this,APP)){
            Toast.makeText(this, "Appen GeofencingAlpga er innstallert", Toast.LENGTH_SHORT).show();
        }else
        {
            Toast.makeText(this, "Appen Geofencing er ikke innstallert", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG,"onBackPressed()");
        //super.onBackPressed(); //Siden vi er Root Appen så er det ikke vits i å trykke onBack, så den gjør heller ikke noe her ifra.
    }

    /*Sjekekr om appen er innstalert, vi må altså sjekke om Monumentvandrings appen er installert, ellers så får ikke brukeren gjordt noe.*/
    public boolean checkIfAppInstalled(Context context, String uri){
        try{
            context.getPackageManager().getApplicationInfo(uri,0);
            return true;
        }
        catch (PackageManager.NameNotFoundException e){
            return false;
        }
    }

    public Context getContext(){
        if(context != null)
            return context;
        else
            return this.getApplicationContext();
    }

}

