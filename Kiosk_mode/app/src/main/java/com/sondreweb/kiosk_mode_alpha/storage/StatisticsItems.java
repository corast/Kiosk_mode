package com.sondreweb.kiosk_mode_alpha.storage;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by sondre on 11-Apr-17.
 */

public class StatisticsItems implements BaseColumns {

    /**
     * The content URI for this table.
     */
    public static final Uri CONTENT_URI =
            Uri.withAppendedPath(
                    StatisticsItemsContract.CONTENT_URI,
                    "statisticsItems"
            );

    /**
     * The mime type of a directory of items.
     */
    public static final String CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE +
                    "/com.sondreweb.kiosk_mode_alpha.storage.statisticsItems";


    /**
     * The mime type of a single item.
     */
    public static final String CONTENT_ITEM_TYPE =
            ContentResolver.CURSOR_ITEM_BASE_TYPE +
                    "/com.sondreweb.kiosk_mode_alpha.storage.statisticsItems";


    /**
     * A projection of all columns
     * in the items table.
     */
    public static final String[] PROJECT_ALL =
            {
                    _ID
            };

    public static final String SORT_ORDER_DEFAULT =
            _ID + " ASC";
}
