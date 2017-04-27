package com.sondreweb.kiosk_mode_alpha.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.adapters.StatisticsAdapter;
import com.sondreweb.kiosk_mode_alpha.storage.SQLiteHelper;
import com.sondreweb.kiosk_mode_alpha.utils.AppUtils;

import java.util.ArrayList;

/**
 * Created by sondre on 25-Apr-17.
 */

public class StatisticsActivity extends AppCompatActivity {

    private static final String TAG = StatisticsAdapter.class.getSimpleName();

    ListView statisticsListView;
    StatisticsAdapter statisticsAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics_overview);
        statisticsListView = (ListView) findViewById(R.id.list_view_statistics);

        ViewStub stub = (ViewStub) findViewById(R.id.vs_statistics_empty);
        statisticsListView.setEmptyView(stub);

        statisticsAdapter = new StatisticsAdapter(getApplicationContext());
        statisticsListView.setAdapter(statisticsAdapter);
        LayoutInflater mInflater = getLayoutInflater();
        ViewGroup myHeader = (ViewGroup) mInflater.inflate(R.layout.list_header_statistics, statisticsListView,false);

        statisticsListView.addHeaderView(myHeader, null, false);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Statistics overview");
    }


    public void initalizeTable(){
        if(! SQLiteHelper.getInstance(getApplicationContext()).checkDataInStatisticsTable()){
            //Vill si at det ikke er noe data i statistikk tablen.
            //Da må vi adde et emty View.
        }else{

        }
    }

    @Override
    protected void onStart() {

        updateStatisticsTable();
        super.onStart();
    }

    private void updateStatisticsTable(){

        if(!statisticsAdapter.isEmpty()){
            statisticsAdapter.clear();
        }

        ArrayList<ContentValues> statisticsList = SQLiteHelper.getInstance(getApplicationContext()).getAllStatistics();
        if(AppUtils.DEBUG){
            Log.d(TAG,statisticsList.toString());
        }

        statisticsAdapter.addAll(statisticsList);



    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //TODO: finn ut om denne fungere.
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
