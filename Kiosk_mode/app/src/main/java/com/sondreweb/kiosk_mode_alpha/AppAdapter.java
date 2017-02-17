package com.sondreweb.kiosk_mode_alpha;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by sondre on 17-Feb-17.
 * Addapter for hvordan hver item i listen skal ligge i fragmentent.
 */

public class AppAdapter extends ArrayAdapter<AppModel> {

    private static final String TAG = AppAdapter.class.getSimpleName();

    private final LayoutInflater inflater;

    public AppAdapter(Context context)
    {
        super(context, android.R.layout.two_line_list_item);

        inflater = LayoutInflater.from(context);

    }

    public static class ViewHolder{
        ImageView icon;
        TextView label;
    }


    public void setData(ArrayList<AppModel> data){
        clear();
        if(data != null){
            addAll(data);
        }
    }

    @Override  //Legg till alle Itemnene inn i Listen vi har lagd.
    public void addAll(Collection<? extends AppModel> items) {
        //sjekker om platformen støtter addAll. Ble lagt til i HoneyComb
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ){
            super.addAll(items);
        }else{
            for(AppModel item : items){
                super.add(item);
            }
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if(convertView == null){
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.list_item_icon_text_test, parent,false);

            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.label = (TextView) convertView.findViewById(R.id.label);

            convertView.setTag(viewHolder);

        }else
        {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        AppModel item = getItem(position);

        viewHolder.icon.setImageDrawable(item.getIcon());
        viewHolder.label.setText(item.getAppName());

        //return super.getView(position, convertView, parent);
        return convertView;
    }
}
