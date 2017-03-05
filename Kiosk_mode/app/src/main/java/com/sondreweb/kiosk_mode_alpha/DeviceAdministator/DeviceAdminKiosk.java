package com.sondreweb.kiosk_mode_alpha.deviceAdministator;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.sondreweb.kiosk_mode_alpha.activities.HomeActivity;
import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;

//import com.sondreweb.kiosk_mode_alpha.utils.AppUtils;
//import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;

/**
 * Created by sondre on 24-Feb-17.
 */

public class DeviceAdminKiosk extends DeviceAdminReceiver{

    public void showToast(Context context, String msg){
        //String status = context.getString(R.string.admin_re, msg);
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    @Override   //Callback for når vi er Device Administrator
    public void onEnabled(Context context, Intent intent) {
        showToast(context,"onEnabled i DeviceAdmin");
        PreferenceUtils.setPrefAdminDevice(true,context);
        super.onEnabled(context, intent);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        //TODO Intercept med passord?
        //Tester med å gå tilbake til home
        /* Intent homeActivity = new Intent(Intent.ACTION_MAIN);
        homeActivity.addCategory(Intent.CATEGORY_ALTERNATIVE);
        homeActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(homeActivity);
        */
        return super.onDisableRequested(context, intent);
    }

    @Override //Callback for når vi ikke er Device Administrator
    public void onDisabled(Context context, Intent intent) {
        showToast(context,"onDisabled i DeviceAdmin");
        PreferenceUtils.setPrefAdminDevice(false,context);
        super.onDisabled(context, intent);
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        super.onPasswordChanged(context, intent);
    }

    @Override
    public void onLockTaskModeEntering(Context context, Intent intent, String pkg) {
        super.onLockTaskModeEntering(context, intent, pkg);
    }

    @Override
    public void onLockTaskModeExiting(Context context, Intent intent) {
        super.onLockTaskModeExiting(context, intent);
    }
}
