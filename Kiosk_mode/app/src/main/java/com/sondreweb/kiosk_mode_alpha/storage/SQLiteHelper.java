package com.sondreweb.kiosk_mode_alpha.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.model.LatLng;
import com.sondreweb.kiosk_mode_alpha.classes.GeofenceClass;
import com.sondreweb.kiosk_mode_alpha.classes.GeofenceStatus;
import com.sondreweb.kiosk_mode_alpha.utils.AppUtils;

import java.util.ArrayList;
import java.util.Collection;
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

    public static final String DATABASE_NAME = "geofence.db";

    //dette er samme som FeedReaderDbHelper fra developer.android om databaser.
    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    //SingelTon object av instansen til denne klassen.
    //så vi kan slippe å close databasen og bare la den stå oppe, og bruke denn viss ledig.
    public static synchronized SQLiteHelper getInstance(Context context){
        if(instance == null){
            instance = new SQLiteHelper(context.getApplicationContext());
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
        //Må lage alle Tabellen dersom det ikke er gjordt.
        GeofenceTable.onCreate(db);
        StatisticsTable.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Oppgaderer tabellene, ved å slette gamle versjoner og lage nye.
        GeofenceTable.onUpgrade(db, oldVersion, newVersion);
        StatisticsTable.onUpgrade(db,oldVersion,newVersion);

        onCreate(db);//lager databasen på nytt igjenn med ny utgave.
    }

    /*
    *   Metode for hente ut alle Geofence og sende de tilbake som en Liste.
    * */

    public List<Geofence> getAllGeofences(){
        SQLiteDatabase db = this.getReadableDatabase();

        ArrayList<Geofence> geofenceArrayList = new ArrayList<>();

        //Henter alle rader fra databasen for denne tabellen.
        String selectQuery = "SELECT * FROM "+ GeofenceTable.TABLE_NAME;


        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.moveToFirst() && cursor.getCount() >= 1){//Flytter oss til først rad dersom den eksisterer
            do {  //Legger til alle Geofence fra database i Arrayet med korrekte atributter.
                geofenceArrayList.add( new Geofence.Builder()
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                        .setCircularRegion(cursor.getDouble(cursor.getColumnIndex(GeofenceTable.COLUMN_LATITUDE)),
                                cursor.getDouble(cursor.getColumnIndex(GeofenceTable.COLUMN_LONGITUDE)),
                                cursor.getFloat(cursor.getColumnIndex(GeofenceTable.COLUMN_RADIUS)))
                        .setRequestId(id_HEAD+cursor.getString(cursor.getColumnIndex(GeofenceTable.COLUMN_GEOFENCE_ID)))
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .build());

            }while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return geofenceArrayList;
        //TODO Lag en liste med GeofenceStatus objecter utifra dette vi returneren her.
    }

    public List<GeofenceClass> getAllGeofencesClass(){

        SQLiteDatabase db = this.getReadableDatabase();

        ArrayList<GeofenceClass> geofenceArrayList = new ArrayList<>();

        String selectQuery = "SELECT * FROM "+GeofenceTable.TABLE_NAME;

        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){//Flytter oss til først rad dersom den eksisterer
            do {  //Legger til alle Geofence fra database i Arrayet med korrekte atributter.
                geofenceArrayList.add( new GeofenceClass(
                        id_HEAD+cursor.getString(cursor.getColumnIndex(GeofenceTable.COLUMN_GEOFENCE_ID)),
                        cursor.getDouble(cursor.getColumnIndex(GeofenceTable.COLUMN_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(GeofenceTable.COLUMN_LONGITUDE)),
                        cursor.getFloat(cursor.getColumnIndex(GeofenceTable.COLUMN_RADIUS)))
                );
            }while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return geofenceArrayList;
        //TODO Lag en liste med GeofenceStatus objecter utifra dette vi returneren her.
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

        long id = db.insertOrThrow(GeofenceTable.TABLE_NAME, null, values);
        db.close();
        return id;
        //vi får en ID tilbake, eller så kaster vi en error.
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

    //For syncing av databasen, Erstatt alle geofence i tabellen vår med disse nye vi får inn.
    public boolean replaceGeofences(List<ContentValues> geofences){
        SQLiteDatabase db = this.getWritableDatabase();

        //Slette all data fra databasen.
        db.execSQL("DELETE FROM "+GeofenceTable.TABLE_NAME); //tømmer databasen.

        for (ContentValues geofence:
                geofences) {
            addGeofence(
                    new LatLng(geofence.getAsDouble(GeofenceTable.COLUMN_LATITUDE),geofence.getAsDouble(GeofenceTable.COLUMN_LONGITUDE)),
                    geofence.getAsInteger(GeofenceTable.COLUMN_RADIUS));

            //Legger til alle Geofence objektene.
            //addGeofence(geofence.getLatLng(), geofence.getRadius());
        }
        return true;
    }

    /*
    *   ¤¤¤¤¤¤¤¤¤¤¤¤StatisticsTable Funskjoner¤¤¤¤¤¤¤¤¤¤¤¤
    * */

    /*
    *   Sjekker om det er noe data i Statistics Tabellen.
    *  */
    public boolean checkDataInStatisticsTable(){
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT count(*) FROM "+ StatisticsTable.TABLE_NAME;

        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            if(cursor.getInt(0) > 0){ //Sjekker om antall rader er mer enn 0.
                return true; //er data
            }
        }
        return false; //er ikke data.
    }

    public Cursor getStatistics(String id, String[] projection, String selection, String[] selectionArgs, String sortOrder){
        SQLiteQueryBuilder sqLiteQueryBuilder = new SQLiteQueryBuilder();
        sqLiteQueryBuilder.setTables(StatisticsTable.TABLE_NAME);

        if(id != null){ //TODO: denne vill ikke fungere, siden vi har 3 primary keys
            sqLiteQueryBuilder.appendWhere(StatisticsTable.COLUMN_VISITOR_ID+" = "+id);
        }

        if(sortOrder == null || sortOrder.equalsIgnoreCase("")){
            sortOrder = StatisticsTable.COLUMN_DATE;
        }

        Cursor cursor = sqLiteQueryBuilder.query(getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);

        return cursor;
    }

    /*
    *   Insert funksjoner.
    *   insert(table,
    *       nullColumnHack,
    *       values);
    * */

    /*
    *   Legg til en statistikk rad.
    * */
    public long addNewStastistic(ContentValues value){
        long id = getWritableDatabase().insert(
                StatisticsTable.TABLE_NAME,
                null,
                value);

        return id;
    }

    /*
    *   Legg til en mengde med statistikk med en gang, mest effektivt.
    * */
    public ArrayList<Long> addAllNewStatics(ContentValues[] values){
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<Long> idList = new ArrayList<>();
        db.beginTransaction();
        try{
            //For hver value, må vi inserte
            for (ContentValues value : values) {
                idList.add(db.insert(StatisticsTable.TABLE_NAME,
                        null,
                        value));
            }
            //Når alle er forsøkt lagt til
            db.setTransactionSuccessful();
        }finally {
            db.endTransaction();
        }
        return idList; //Dersom listen inneholde noen som er mindre enn 0, så er det error.
    }

    /*
    *   Returnere en liste med all statistikk som ligger i datasen nå.
    * */
    public synchronized ArrayList<ContentValues> getAllStatistics(){
        ArrayList<ContentValues> statisticsList = new ArrayList<>();

        SQLiteDatabase dbReadable = this.getReadableDatabase();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(KioskDbContract.Statistics.TABLE_NAME);

        Cursor cursor = queryBuilder.query(dbReadable, null, null, null,
                null, null, null);

        ContentValues contentValues;

        if(cursor.moveToFirst()){//Flytter oss til først rad dersom den eksisterer
            do {  //Legger til alle Geofence fra database i Arrayet med korrekte atributter.
                contentValues = new ContentValues(); //Lager en ny ContentValue å lagre verdiene våre inn i middeltidlig.
                //Legger inn alle verdiene fra raden vi står på nå.
                contentValues.put(StatisticsTable.COLUMN_MONUMENT,cursor.getInt(cursor.getColumnIndex(StatisticsTable.COLUMN_MONUMENT)));
                contentValues.put(StatisticsTable.COLUMN_VISITOR_ID,cursor.getInt(cursor.getColumnIndex(StatisticsTable.COLUMN_VISITOR_ID)));
                contentValues.put(StatisticsTable.COLUMN_DATE,cursor.getString(cursor.getColumnIndex(StatisticsTable.COLUMN_DATE)));
                contentValues.put(StatisticsTable.COLUMN_TIME,cursor.getInt(cursor.getColumnIndex(StatisticsTable.COLUMN_TIME)));
                //Setter dette objektet med verdiene inn i en ArrayList.
                statisticsList.add(contentValues);
            }while (cursor.moveToNext()); //beveger oss til neste rad.
        }
            //Returnerer ArrayListen med verdiene fra alle radene.
        return statisticsList;
    }

    /*
    *   Funksjon for å tømme Statistics tabellen, for bruk etter vi har sent successfully til databasen.
    *   Men burde ikke kjøre for ofte, ettersom det kan være greit å holde på duplikater og så sile de ut fra databasen.
    * */
    public int emptyStatisticsTable(){
        SQLiteDatabase dbWritable = this.getWritableDatabase();
        //Returnere antall rader som var påvirket av delete setningen.
        return dbWritable.delete(StatisticsTable.TABLE_NAME,null,null);
    }

}
