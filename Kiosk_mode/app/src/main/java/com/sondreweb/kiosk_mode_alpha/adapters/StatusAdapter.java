package com.sondreweb.kiosk_mode_alpha.adapters;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.sondreweb.kiosk_mode_alpha.AppModel;
import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.StatusInfo;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by sondre on 23-Mar-17.
 */

public class StatusAdapter extends ArrayAdapter<StatusInfo>{

    private static final String TAG = AppAdapter.class.getSimpleName();

    private final LayoutInflater inflater;

    public StatusAdapter(Context context)
    {
        super(context, android.R.layout.two_line_list_item);

        inflater = LayoutInflater.from(context);

    }

    public static class ViewHolder{
        ImageButton icon;
        TextView label;
    }


    public void setData(ArrayList<StatusInfo> data){
        //clear();
        if(data != null){
            addAll(data);
        }
    }

    @Override  //Legg till alle Itemnene inn i Listen vi har lagd.
    public void addAll(Collection<? extends StatusInfo> items) {
        //sjekker om platformen støtter addAll. Ble lagt til i HoneyCombBuild.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ){
            super.addAll(items);
        }else{//ellers må vi gå gjennom alle.
            for(StatusInfo item : items){
                super.add(item);
            }
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        AppAdapter.ViewHolder viewHolder;

        //Dersom convertView ikke er laget enda.
        if(convertView == null){
            viewHolder = new AppAdapter.ViewHolder();
            convertView = inflater.inflate(R.layout.gridview_status, parent,false);

            viewHolder.icon = (ImageButton) convertView.findViewById(R.id.grid_status_image_button);
            viewHolder.label = (TextView) convertView.findViewById(R.id.grid_status_name);

            convertView.setTag(viewHolder);
        }
        else
        {
            viewHolder = (AppAdapter.ViewHolder) convertView.getTag();
        }

        StatusInfo status = getItem(position);

        viewHolder.icon.setImageResource(status.getImageDrawable());
        viewHolder.label.setText(status.getName());

        return convertView;
    }

    //TODO: fiks slik at vi kan trykke på de ulike statusene.
    //TODO: Oppdater statusene hver gang vi går inn på Home.

    @Nullable
    @Override
    public StatusInfo getItem(int position) {
        return super.getItem(position);
    }
}
