package com.sondreweb.kiosk_mode_alpha.DeviceAdministator;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

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
        super.onEnabled(context, intent);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return super.onDisableRequested(context, intent);
    }

    @Override //Callback for når vi ikke er Device Administrator
    public void onDisabled(Context context, Intent intent) {
        showToast(context,"onDisabled i DeviceAdmin");
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
