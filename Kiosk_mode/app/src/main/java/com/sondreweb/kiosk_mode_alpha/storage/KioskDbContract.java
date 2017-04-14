package com.sondreweb.kiosk_mode_alpha.storage;

import android.content.ContentResolver;
import android.net.Uri;

/**
 * Created by sondre on 11-Apr-17.
 */

public final class KioskDbContract {

    //TODO: lag ferdig denne til Martin sin app.


    /*
    *   Authority.
    * */
    public static final String AUTHORITY = "com.sondreweb.kiosk_mode_alpha.storage.CustomContentProvider";

    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY);


    public static final String SORT_ORDER_DEFAULT =
            StatisticsTable.COLUMN_VISITOR_ID + " ASC";

    //TODO: lage denne slik at det er enkelt for Martin å bruke ContentProvideren.

    public static final class Statistics {
        public static final String TABLE_NAME ="Statistics";

        //brukes for å velge denne tabellen.
        public static final Uri CONTENT_URI = Uri.withAppendedPath(KioskDbContract.CONTENT_URI,TABLE_NAME);


        //TODO: fiks disse 15.april
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "232";

        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "232";

        /* Kolonnene som finnes i tabellen.*/
        public static final String COLUMN_MONUMENT = "monument";

        public static final String COLUMN_VISITOR_ID = "visitor_nr";

        public static final String COLUMN_TIME = "time";

        public static final String COLUMN_DATE = "date";
    }


    //Constant for the statistics Table of
}
