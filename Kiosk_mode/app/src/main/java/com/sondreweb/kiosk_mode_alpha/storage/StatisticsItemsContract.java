package com.sondreweb.kiosk_mode_alpha.storage;

import android.content.ContentResolver;
import android.net.Uri;

/**
 * Created by sondre on 11-Apr-17.
 */

public final class StatisticsItemsContract {

    /*
    *   Authority.
    * */
    public static final String AUTHORITY = "com.sondreweb.kiosk_mode_alpha.storage.statisticsItems";

    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY);


    //TODO: lage denne slik at det er enkelt for Martin å bruke ContentProvideren.



    //Constant for the statistics Table of
}
