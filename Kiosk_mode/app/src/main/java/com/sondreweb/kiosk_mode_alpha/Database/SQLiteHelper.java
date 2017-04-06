package com.sondreweb.kiosk_mode_alpha.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.model.LatLng;
import com.sondreweb.kiosk_mode_alpha.classes.GeofenceClass;
import com.sondreweb.kiosk_mode_alpha.services.GeofenceTransitionService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sondre on 30-Mar-17.
 */

public class SQLiteHelper extends SQLiteOpenHelper {

    public static final String TAG = SQLiteHelper.class.getName();

    private static final String id_HEAD = "geofence_";

    private static final int DATABASE_VERSION  = 1; //ved forandring av database schemet må vi forandre denne.
    //realistisk sett, så burde egentlig hele databasen byttes ut ved forandring, ivertfall ikke der hvor profil data ligger.

    private static SQLiteHelper instance; //viss databasen er åpen kan vi bare bruke denne.

    private static Context sContext = null;

    public static final String DATABASE_NAME = "geofence.db";

    //dette er samme som FeedReaderDbHelper fra developer.android om databaser.
    private SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //så vi kan slippe å close databasen og bare la den stå oppe, og bruke denn viss ledig.
    public static synchronized SQLiteHelper getInstance(Context context){
        if(instance == null){
            instance = new SQLiteHelper(context);
            sContext = context;
        }
        return instance;
    }

    @Override
    public synchronized void close() { //lukker databasen
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        GeofenceTable.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        GeofenceTable.onUpgrade(db, oldVersion, newVersion);
    }

    /*
    *   Metode for hente ut alle Geofence og sende de tilbake som en Liste.
    * */

    public List<Geofence> getAllGeofences(){
        SQLiteDatabase db = this.getReadableDatabase();

        ArrayList<Geofence> geofenceArrayList = new ArrayList<>();

        String selectQuery = "SELECT * FROM "+GeofenceTable.TABLE_NAME;

        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.moveToFirst() && cursor.getCount() <= 1){//Flytter oss til først rad dersom den eksisterer
            do {  //Legger til alle Geofence fra database i Arrayet med korrekte atributter.
                geofenceArrayList.add( new Geofence.Builder()
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                        .setCircularRegion(cursor.getDouble(cursor.getColumnIndex(GeofenceTable.COLUMN_LATITUDE)),
                                cursor.getDouble(cursor.getColumnIndex(GeofenceTable.COLUMN_LONGITUDE)),
                                cursor.getFloat(cursor.getColumnIndex(GeofenceTable.COLUMN_RADIUS)))
                        .setRequestId(id_HEAD+cursor.getString(cursor.getColumnIndex(GeofenceTable.COLUMN_GEOFENCE_ID)))
                        .build());

            }while (cursor.moveToNext());
        }
        cursor.close();
        return geofenceArrayList;
    }

    /*
    *   Metode for å legge til et nytt Geofence til Listen
    * */

    public long addGeofence(LatLng latLng, float radius){ //vi må sørge for at disse sammenlagt ikke er like.

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(GeofenceTable.COLUMN_LATITUDE,latLng.latitude);
        values.put(GeofenceTable.COLUMN_LONGITUDE, latLng.longitude);
        values.put(GeofenceTable.COLUMN_RADIUS, radius);

        return db.insertOrThrow(GeofenceTable.TABLE_NAME, null, values); //vi får en ID tilbake, eller så kaster vi en error.
    }

    /*
    *   Vider utvikling, legge til flere typer Geofence, foreksempel en egen for Vandring.
    * */

    //legg til et array av Geofence med en gang, og fjern de gamle?
    public List<Long> addGeofences(List<Geofence> geofences){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        //TODO: legg til flere geofence samtidig.

        return new ArrayList<>();
    }

    //TODO sjekk gamel versjon av databasen, for om den trenger syncing eller ikke.

    //For syncing av databasen.
    public boolean replaceGeofences(List<GeofenceClass> geofences){
        SQLiteDatabase db = this.getWritableDatabase();
        //TODO lagre en kopi av gamel data også

        //Slette all data fra databasen.
        db.execSQL("DELETE FROM "+GeofenceTable.TABLE_NAME);

        for (GeofenceClass geofence:
                geofences) {
            //Legger til alle Geofence objektene.
            addGeofence(geofence.getLatLng(), geofence.getRadius());
        }

        for (GeofenceClass geofence:
             geofences) {
            //Legger til alle Geofence objektene.
            addGeofence(geofence.getLatLng(), geofence.getRadius());
        }



        return true;
    }



}
