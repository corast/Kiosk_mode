package com.sondreweb.kiosk_mode_alpha.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.sondreweb.kiosk_mode_alpha.R;

/**
 * Created by sondre on 25-Apr-17.
 */

public class StatisticsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_statistics_overview);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Statistics overview");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            Intent upIntent = NavUtils.getParentActivityIntent(this);
            if(NavUtils.shouldUpRecreateTask(this, upIntent)){
                TaskStackBuilder.create(this)
                        .addNextIntentWithParentStack(upIntent)
                        .startActivities();
            }else{
                NavUtils.navigateUpTo(this, upIntent);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
