package com.sondreweb.kiosk_mode_alpha.Settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.sondreweb.kiosk_mode_alpha.R;

import java.util.List;

/**
 * Created by sondre on 02-Mar-17.
 */

public class AdminPanel extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_header, target);
        super.onBuildHeaders(target);
    }
}
