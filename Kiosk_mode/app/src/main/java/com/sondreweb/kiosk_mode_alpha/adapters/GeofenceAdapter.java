package com.sondreweb.kiosk_mode_alpha.adapters;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.location.Geofence;
import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.classes.GeofenceClass;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by sondre on 21-Apr-17.
 */

public class GeofenceAdapter extends ArrayAdapter<GeofenceClass> {

    private final LayoutInflater inflater;

    public GeofenceAdapter(Context context)
    {
        super(context, android.R.layout.two_line_list_item);

        inflater = LayoutInflater.from(context);
    }

    private static class ViewHolder{

        TextView latitude;
        TextView longitude;
        TextView radius;
    }

    @Override  //Legg till alle Itemnene inn i Listen vi har lagd.
    public void addAll(Collection<? extends GeofenceClass> items) {
        //sjekker om platformen støtter addAll. Ble lagt til i HoneyCombBuild.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ){
            super.addAll(items);
        }else{//ellers må vi gå gjennom alle.
            for(GeofenceClass item : items){
                super.add(item);
            }
        }
    }
    public void setData(List<GeofenceClass> data){
        //clear();
        if(data != null){
            addAll(data);
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        GeofenceAdapter.ViewHolder viewHolder;

        //Dersom convertView ikke er laget enda.
        if (convertView == null) {
            viewHolder = new GeofenceAdapter.ViewHolder();
            convertView = inflater.inflate(R.layout.list_item_geofence, parent, false);

            viewHolder.latitude = (TextView) convertView.findViewById(R.id.text_geofence_latitude);
            viewHolder.longitude = (TextView) convertView.findViewById(R.id.text_geofence_longitude);
            viewHolder.radius = (TextView) convertView.findViewById(R.id.text_geofence_radius);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (GeofenceAdapter.ViewHolder) convertView.getTag();
        }

        GeofenceClass geofence = getItem(position);

        //TODO: forandre på iconene basert på hva vi trykker på.
        if (geofence != null) {
            viewHolder.latitude.setText(String.format("%.7f",geofence.getLatLng().latitude));
            viewHolder.longitude.setText(String.format("%.7f",geofence.getLatLng().longitude));
            viewHolder.radius.setText(String.format("%.0f",geofence.getRadius()));
        }
        return convertView;
    }
}
