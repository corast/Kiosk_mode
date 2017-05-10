package com.sondreweb.kiosk_mode_alpha.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.classes.GeofenceClass;
import com.sondreweb.kiosk_mode_alpha.storage.StatisticsTable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Adapter for hvordan hver statistik rad skal vises i en liste.
 */

public class StatisticsAdapter extends ArrayAdapter<ContentValues>{

    private final LayoutInflater inflater;

    public StatisticsAdapter(Context context)
    {
        super(context, android.R.layout.two_line_list_item);

        inflater = LayoutInflater.from(context);

    }

    private static class ViewHolder{

        TextView monument;
        TextView visitor_id;
        TextView date;
        TextView time;
    }

    @Override  //Legg till alle Itemnene inn i Listen vi har lagd.
    public void addAll(Collection<? extends ContentValues> items) {
        //sjekker om platformen støtter addAll. Ble lagt til i HoneyCombBuild.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ){
            super.addAll(items);
        }else{//ellers må vi gå gjennom alle.
            for(ContentValues value : items){
                super.add(value);
            }
        }
    }
    public void setData(ArrayList<ContentValues> data){
        //clear();
        if(data != null){
            addAll(data);
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        StatisticsAdapter.ViewHolder viewHolder;
        //Dersom convertView ikke er laget enda.
        if (convertView == null) {
            viewHolder = new StatisticsAdapter.ViewHolder();
            convertView = inflater.inflate(R.layout.list_item_statistics, parent, false);
            //TODO:id
            viewHolder.monument = (TextView) convertView.findViewById(R.id.text_statistics_monument);
            viewHolder.visitor_id = (TextView) convertView.findViewById(R.id.text_statistics_visitor_id);
            viewHolder.date = (TextView) convertView.findViewById(R.id.text_statistics_date);
            viewHolder.time = (TextView) convertView.findViewById(R.id.text_statistics_time);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (StatisticsAdapter.ViewHolder) convertView.getTag();
        }

        ContentValues contentValues = getItem(position);

        if (contentValues != null) {
            //henter ut verdiene fra contentValues og putter de inn i tilsvarende TextView på item_list_staticis.xml layouten for hver rad med data.
            viewHolder.monument.setText(contentValues.getAsString(StatisticsTable.COLUMN_MONUMENT));
            viewHolder.visitor_id.setText(contentValues.getAsString(StatisticsTable.COLUMN_VISITOR_ID));
            viewHolder.date.setText(contentValues.getAsString(StatisticsTable.COLUMN_DATE));
            viewHolder.time.setText(contentValues.getAsString(StatisticsTable.COLUMN_TIME));
        }
        return convertView;
    }
}
