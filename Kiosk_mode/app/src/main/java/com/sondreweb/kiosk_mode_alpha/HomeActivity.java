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

import com.sondreweb.kiosk_mode_alpha.DeviceAdministator.DeviceAdminKiosk;

/**
 * Created by sondre on 16-Feb-17.
 *
 * Home screen appen, denne har ansvar for å vise  tilgjengelige applikasjoner.
 * TODO: Lag en knapp som starter Kiosk mode(altså starter opp monumentvandringen, vi har ikke lov å gå ut av denne.
 */
public class HomeActivity extends FragmentActivity {


    public final static String TAG = HomeActivity.class.getSimpleName();
    private final static String APP = "com.sondreweb.geofencingalpha";

    private View decorView;
    private PackageManager packageManager;
    private Context context;

    private DevicePolicyManager devicePolicyManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //Lager en ny Component Indentifier fra en classe. Men hvorfor?
            //TODO: finn ut hva dette faktisk gjør.
        ComponentName deviceAdmin = new ComponentName(this, DeviceAdminKiosk.class);

        //Henter DevicePolicMangar, brukes for å sjekke om vi er admin osl.
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        if(devicePolicyManager.isAdminActive(deviceAdmin)){
            PreferenceUtils.setPrefAdminDevice(true, this); //dersom vi er admin, så kan vi sette at vi faktisk er det i instillingene:
            Toast.makeText(this,"ikke Admin",Toast.LENGTH_SHORT);
        }
        else
        {
            PreferenceUtils.setPrefAdminDevice(false,this); //dersom vi ikk er admin, så lagrer vi dette til senere bruk.
            Log.d(TAG,"vi er ikke admin");
        }

        if(devicePolicyManager.isDeviceOwnerApp(getPackageName())){
           Log.d(TAG, "Vi er Device owner");
        }
        else
        {
            Toast.makeText(this,"Not device owner",Toast.LENGTH_SHORT);
        }
        decorView = getWindow().getDecorView();
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
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume()");
        hideSystemUiTest();
    }

    public void hideSystemUiTest(){
        //decorView.setSystemUiVisibility(
                /*View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                */
        //);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG,"onBackPressed()");
        //super.onBackPressed(); //Siden vi er Bunnen av aplikasjons stacken, så er det ikke vits i å trykke onBack, den gjør heller ikke noe her ifra.
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

    public void startMonumentVandring(View view) {
       //vi starter MonumentVandringen
        PreferenceUtils.setKioskModeActive(true, this);
        StartActivity(APP);
    }

    //TODO: Bruke en App object istedet, litt tryggere på errors.
    public void StartActivity(String packageName){
        Log.d(TAG,"StartActivity: "+ packageName);
        Intent intent = this.getPackageManager().getLaunchIntentForPackage(packageName);

        if(intent != null ){
            startActivity(intent);
        }
    }
}

