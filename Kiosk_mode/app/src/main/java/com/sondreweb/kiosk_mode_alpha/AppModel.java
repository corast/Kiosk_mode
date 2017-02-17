package com.sondreweb.kiosk_mode_alpha;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.text.Collator;
import java.util.Comparator;

/**
 * Created by sondre on 17-Feb-17.
 *
 *   Fra: https://developer.android.com/reference/android/content/AsyncTaskLoader.html
 *
 *   App objecter.
 */

public class AppModel {

    private final Context context;
    private final ApplicationInfo info;

    private String appLabel;

    private String appName;

    private Drawable icon;

    private boolean mounted;
    private final File apkFile;

    public AppModel(Context context, ApplicationInfo info){
        this.context = context;
        this.info = info;
        //testing på å få tak i navnet på applikajonen.
        this.appName = context.getPackageManager().getApplicationLabel(info).toString();
        apkFile = new File(info.sourceDir);
    }

    public String getAppLabel(){
        return appLabel;
    }

    public ApplicationInfo getAppInfo(){
        return info;
    }

    public String getApplicationPackageName(){
        return getAppInfo().packageName;
    }

    public String getAppName(){
        return appName;
    }

    public Drawable getIcon(){
        if(icon == null){ //legger til Iconet som vi finner fra APK filen
            if(apkFile.exists()){
                icon = info.loadIcon(context.getPackageManager());
                return icon;
            }else
                mounted = false;
        } else if(!mounted) //viss Appen ikke er mounted, reload iconet
        {
            if(apkFile.exists()){
                mounted = true;
                icon = info.loadIcon(context.getPackageManager());
                return icon;
            }
        }else
        {
            return icon;
        }

        //returnere default App icon, dersom vi ikke finner noen iconer.
        return context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
    }

    /**
    * Sett Label fra context til Applikasjonen.
    */
    void loadLabel(Context context){
        if(appLabel == null || !mounted){
            mounted = false;
            appLabel = info.packageName;
        }else
        {
            mounted = true;
            CharSequence label = info.loadLabel(context.getPackageManager());
            if(label != null){
                appLabel = label.toString();
            }else{
                appLabel = info.packageName;
            }
        }
    }

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<AppModel> ALPHA_COMPARATOR = new Comparator<AppModel>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppModel object1, AppModel object2) {
            return sCollator.compare(object1.getAppLabel(), object2.getAppLabel());
        }
    };

    @Override
    public String toString() {
        return " "+appLabel+" ";
    }
}
