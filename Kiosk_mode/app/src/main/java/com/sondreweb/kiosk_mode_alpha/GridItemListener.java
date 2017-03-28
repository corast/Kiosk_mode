package com.sondreweb.kiosk_mode_alpha;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

/**
 * Created by sondre on 28-Mar-17.
 */

public class GridItemListener implements AdapterView.OnItemClickListener {

    public static final String TAG = GridItemListener.class.getSimpleName();

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG,view.toString());

    }


}
