package com.sondreweb.kiosk_mode_alpha.adapters;

import android.content.Context;
import android.media.Image;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.classes.StatusInfo;

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

    private static class ViewHolder{

        ImageView icon;
        TextView label;
        TextView info;
        RelativeLayout rLayout;
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

        StatusAdapter.ViewHolder viewHolder;

        //Dersom convertView ikke er laget enda.
        if(convertView == null){
            viewHolder = new StatusAdapter.ViewHolder();
            convertView = inflater.inflate(R.layout.gridview_status, parent,false);

            viewHolder.icon = (ImageView) convertView.findViewById(R.id.grid_status_image);
            viewHolder.label = (TextView) convertView.findViewById(R.id.grid_status_name);
            viewHolder.info = (TextView) convertView.findViewById(R.id.grid_status_info);

            viewHolder.rLayout = (RelativeLayout) convertView.findViewById(R.id.relative_layout_grid_status);

            convertView.setTag(viewHolder);
        }
        else
        {
            viewHolder = (StatusAdapter.ViewHolder) convertView.getTag();
        }

        StatusInfo status = getItem(position);

        //TODO: forandre på iconene basert på hva vi trykker på.
        if(status != null) {
            if (status.getStatus()) { //siden statusen er aktiv og klar.
                viewHolder.rLayout.setBackgroundResource(R.color.lightGreen);
            }else
            {
                viewHolder.rLayout.setBackgroundResource(R.color.redWard);
            }

            viewHolder.icon.setImageResource(status.getImageDrawable());
            viewHolder.label.setText(status.getName());
            viewHolder.info.setText(status.getInfo());
        }
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
