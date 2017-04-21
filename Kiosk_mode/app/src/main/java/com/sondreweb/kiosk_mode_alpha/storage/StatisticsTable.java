package com.sondreweb.kiosk_mode_alpha.storage;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * Created by sondre on 11-Apr-17.
 */

public class StatisticsTable {

    public static final String TAG = StatisticsTable.class.getName();

    public static final String TABLE_NAME ="Statistics";

    public static final String COLUMN_MONUMENT = "monument";

    public static final String COLUMN_VISITOR_ID = "visitor_nr";

    public static final String COLUMN_TIME = "time";

    public static final String COLUMN_DATE = "date";

    public static final String DATABASE_CREATE_STATISTICS =
            "create table "+ TABLE_NAME + "(" +
                    COLUMN_MONUMENT +" integer not null, " +
                    COLUMN_VISITOR_ID +" integer not null, " +
                    COLUMN_DATE + " text not null, " +
                    COLUMN_TIME + " integer not null, " +
                    " PRIMARY KEY("+ COLUMN_MONUMENT+", "+COLUMN_VISITOR_ID+", "+COLUMN_DATE+")"+
                    ");";
    /*
    *   create table Statistics(
    *       monument integer not null,
    *       visitor_nr integer not null,
    *       date integer not null,
    *       time integer not null,
    *       PRIMARY KEY(monument, visitor_nr, date)
    *       );
    * */

    public static void onCreate(SQLiteDatabase database){
        database.execSQL(DATABASE_CREATE_STATISTICS);
        Log.d(TAG, "onCreate StatisticsTable");
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion){
        Log.v(TAG, "Removes all data from table:"+ TABLE_NAME +" Upgrade table from version: "+oldVersion+" To: "+newVersion);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME +";"); //drop table og bytter ut med no annet, viss vi forandrer versjonen
    }
}
