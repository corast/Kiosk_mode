package com.sondreweb.kiosk_mode_alpha;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

/**
 * Created by sondre on 16-Feb-17.
 */
public class HomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }

    public void showApps(View v) {
        Intent intent = new Intent(this, AppListActivity.class);
        //startActivity(intent);
        Toast.makeText(this, "Show app virker ikke ....", Toast.LENGTH_SHORT).show();
    }
}

