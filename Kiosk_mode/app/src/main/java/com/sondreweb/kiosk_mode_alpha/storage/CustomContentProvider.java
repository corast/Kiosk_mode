package com.sondreweb.kiosk_mode_alpha.storage;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.sondreweb.kiosk_mode_alpha.jobscheduler.SynchJobService;
import com.sondreweb.kiosk_mode_alpha.utils.AppUtils;
import com.sondreweb.kiosk_mode_alpha.storage.KioskDbContract.Statistics;
import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sondre on 10-Apr-17.
 * Skal ha ansvar for å lagre data til en database eller hva nå enn vi skal lagrde.
 * Denne skal brukes for å lagre statistikken vi har
 */

public class CustomContentProvider extends ContentProvider {

    private static final String TAG = CustomContentProvider.class.getSimpleName();

    //Navn på provideren vår.
    static final String PROVIDER_NAME =
            "com.sondreweb.kiosk_mode_alpha.storage.CustomContentProvider";

    /*
    *   Assigned to a content provider so any application can access it
    *   cpcontacts is the virtual directory in the provider
    * */

    /*
    *   content://authority/optionalPath/optionalId
    *    SCHEME: alltid "content":
    *    AUTHORITY : Authorities have to be unique for every content provider.
    *    Thus the naming conventions should follow the Java package name rules.
    *    That is you should use the reversed domain name of your organization plus a qualifier for each and every content provider you publish.
    *    PATH: is used to distinguish the kinds of data your content provider offers.
    *    The content provider for Android’s mediastore, for example, distinguishes between audio files,
    *    video files and images using different paths for each of these types of media.
    *    This way a content provider can support different types of data that are nevertheless related.
    *    For totally unrelated data though you should use different content providers – and thus different authorities.
    *    This is usually the table name with which queries will interact to which this content URI is passed.
    *    ID: if present – must be numeric. The id is used whenever you want to access a single record (e.g. a specific video file).
    * */

    //Helper constants for use with the UriMatcher.
    private static final int STATISTIKK_ID = 1; //For å lese ut en verdi.
    private static final int STATISTIKK_LIST = 2; //For å inserte en eller flere verdier eller lese ut.
    /*
    *   Eksempel viss vi har flere Tabeller vi vill dele data fra.
    *   private static final int TABEL_NAME_ID = 3
    *   private static final int TABEL_NAME_LIST = 4
    * */

    //content://authority/optionalPath/optionalId
    //lager root noden for URI treet.

    static final UriMatcher URI_MATCHER;
    static{
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        /*
        URI_MATCHER.addURI(KioskDbContract.AUTHORITY,
                "statistikk",+
                        STATISTIKK_ID);
        */
        //Når vi skal ha tak i kunn ett element med en ID.

        URI_MATCHER.addURI(
                PROVIDER_NAME,
                KioskDbContract.Statistics.TABLE_NAME,
                STATISTIKK_LIST
        );

        URI_MATCHER.addURI(
                PROVIDER_NAME,
                KioskDbContract.Statistics.TABLE_NAME+"/#",
                STATISTIKK_ID
        );
    }

    private SQLiteHelper sqLiteHelper = null;


    private final ThreadLocal<Boolean> mIsInBatchMode = new ThreadLocal<Boolean>();

    /*
    *   Prepares the content provider
    *   return:  true if the provider was successfully loaded, false otherwise
    * */
    @Override
    public boolean onCreate() {
        //initialisere databasen vår.

        sqLiteHelper = SQLiteHelper.getInstance(getContext()); //lager databasen dersom det ikke er gjordt.
        sqLiteHelper.close();
        /*
        *   A content provider is created when its hosting process is created,
        *   and remains around for as long as the process does, so there is no need to close the database --
        *   it will get closed as part of the kernel cleaning up the process's resources when the process is killed.
        *    -En eller annen Engineer fra Google.
        * */
        return true;
    }

    @Override
    public void onLowMemory() {
        //Når vi har lite med minne, hva gjør vi da?
        /*
        *   Ved lite minne, må vi be brukeren gå tilbake til Resepsjonen og bytte ut enheten, siden denne må restartet slik at Cashe kan cleares kanskje?
        * */
        super.onLowMemory();
    }

    /*
    *   Returns the MIME type for this URI
    *   Brukes når man URI_MATCHER.match(uri).
    * */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        // FIXME: 14-Apr-17
        Log.d(TAG,"getType: "+uri);
        switch (URI_MATCHER.match(uri)){
            case STATISTIKK_ID:
                Log.d(TAG,"getType: case: "+STATISTIKK_ID);
                return Statistics.CONTENT_ITEM_TYPE;
            case STATISTIKK_LIST:
                Log.d(TAG,"getType: case: "+STATISTIKK_LIST);
                return Statistics.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported URI "+uri);
        }

        /*
        switch (URI_MATCHER.match(uri)){
            case STATISTIKK_LIST ://dersom vi skal ha tak i en eller flere rader i tabellen.
                /*  CURSOR_DIR_BASE_TYPE= vnd.android.cursor.dir
                //   "vnd.android.cursor.dir/com.sondreweb.kiosk_mode_alpha.storage.statisticsItems"
                //   "vnd.android.cursor.dir/vnd.com.androidcontentproviderdemo.androidcontentprovider.provider.images";
                //return StatisticsItems.CONTENT_TYPE;
            case STATISTIKK_ID: //Dersom vi skal ha tak i en rad
                /*  CURSOR_ITEM_BASE_TYPE= vnd.android.cursor.item
                  // "vnd.android.cursor.item/com.sondreweb.kiosk_mode_alpha.storage.statisticsItems

                //return StatisticsItems.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported URI "+uri);
        } */
    }

    //Sjekker at vi fikk lagt til i databasen riktig, og returnere Uri adressen på denne.
    private Uri getUriForId(long id, Uri uri){
        if(id > 0) {
            Uri itemUri = ContentUris.withAppendedId(uri,id);
            if(!isInBatchMode()){
                getContext()
                        .getContentResolver().notifyChange(itemUri, null);
            }

            return itemUri;
        }
        /*
        *   Dataene ble ikke vellykket inserted.
        *   Dataene er på feil format for å legge til databasen mest sannsynlig.
        * */
        throw new SQLException(
                "problem while inserting into uri: "+uri);
    }

        /*
        *   Modifies data, men dett skal vi ikke gjøre med statistikken uansett.
        * */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    /*
    *   Return records based on selection criteria
    * */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        if(AppUtils.DEBUG) {
            Log.d(TAG, "query Uri: " + uri.toString() + " URI_MATCHER int:" + URI_MATCHER.match(uri));
        }
        SQLiteDatabase databaseReadable = SQLiteHelper.getInstance(getContext()).getReadableDatabase();

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        boolean useAuthorityUri = false;
        switch (URI_MATCHER.match(uri)){
            case STATISTIKK_LIST: //Vill si at vi skal lage en cursor hvor vi skal ha tilbake en eller flere elementer.
                queryBuilder.setTables(StatisticsTable.TABLE_NAME);
                if(TextUtils.isEmpty(sortOrder)){
                    sortOrder = Statistics.SORT_ORDER_DEFAULT; //Sorter på visitor_id ASC.
                }
                break;

            //DENNE ER IKKE I BRUK. Ikke lagt til som URI engang.
            case STATISTIKK_ID: //Vill si at vi skal lage en cursor hvor vi skal ha tilbake kunn et element.
                //Denne er ikke teste ut, så fungere mest sannsynlig ikke som forventet.
                //TODO: Finn ut hvordan vi velger hvilket case som skal gjøres.
                queryBuilder.setTables(StatisticsTable.TABLE_NAME);
                //TODO: Finn ut hvordan vi best kan finne bare en data, siden dette ikke er unikt.
                //uri.getLastPathSegment() er det som kommer etter /# så altså hva som står i #, et tall for primary key eller string. Tror jeg ivetfall.
                //queryBuilder.appendWhere(StatisticsTable.COLUMN_VISITOR_ID + " = " + uri.getLastPathSegment());
                //Vi må da sende inn de tre verdiene vi trenger å sjekke imot for å finne den uniqe raden.
                if(selectionArgs != null && selectionArgs.length == 3){
                    queryBuilder.appendWhere(
                            Statistics.COLUMN_MONUMENT +" = " + selectionArgs[0] +" AND " +
                                    Statistics.COLUMN_VISITOR_ID +" = " +selectionArgs[1] + " AND " +
                                    Statistics.COLUMN_DATE + " = " + selectionArgs[2]);
                }else{
                    throw new IllegalArgumentException("Too few selectionArguments, need monunent, visitor_id and date !");
                }
                break;
            default: //Støtter kunn å hente ut hele tabellen enn så lenge.
                throw new IllegalArgumentException("Unsupporter Uri" + uri);
        }

        // logQuere for debugging.
        logQuery(queryBuilder,  projection, selection, sortOrder);

        Cursor cursor = queryBuilder.query(databaseReadable, projection, selection, selectionArgs,
                null, null, sortOrder);
        try {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }catch (NullPointerException e){
            Log.e(TAG,e.getLocalizedMessage());
        }
        /*
        // if we want to be notified of any changes to database:
        if (useAuthorityUri) {
            cursor.setNotificationUri(getContext().getContentResolver(), KioskDbContract.CONTENT_URI);
        }
        else {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        } */
        databaseReadable.close();
        return cursor;

    /*
        String id = null;
        if(URI_MATCHER.match(uri) == STATISTIKK_ID){
            id = uri.getPathSegments().get(1);
        }
        return sqLiteHelper.getStatistics(id,projection,selection,selectionArgs,sortOrder);
        */
    }
    /*
    * Rett fra grokkingandroid sin kode
    * https://bitbucket.org/grokkingandroid/cpsample/src/c75f7d61f80cf41f009aca8ad346eddf6b3a13e7/src/com/grokkingandroid/sampleapp/samples/data/contentprovider/provider/LentItemsProvider.java?at=master&fileviewer=file-view-default
    * */

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void logQuery(SQLiteQueryBuilder builder, String[] projection, String selection, String sortOrder) {
        if (AppUtils.DEBUG) {
            Log.v("cpsample", "query: " + builder.buildQuery(projection, selection, null, null, sortOrder, null));
        }
    }

    /*
    *   Deletes records
    * */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    /*
    *   For å legge til flere enn en value/rad. Men ikke testet ut, siden bulkInsert var enklere å bruke.
    * */
    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(
            @NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabase db = SQLiteHelper.getInstance(getContext()).getWritableDatabase();

        db.beginTransaction();
        try {
            final ContentProviderResult[] returnResult = super.applyBatch(operations);
            db.setTransactionSuccessful();
            getContext().getContentResolver().notifyChange(KioskDbContract.CONTENT_URI, null);

            return returnResult;
        }
        finally {
            mIsInBatchMode.remove();
            db.endTransaction();
            db.close();
        }
    }

    private boolean isInBatchMode() {
        return mIsInBatchMode.get() != null && mIsInBatchMode.get();
    }

    /*
 *   Adds records
 *   values kan komme inn som et Statistik objekt kanskje?
 * */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @NonNull ContentValues values) {
        SQLiteDatabase databaseWritable = SQLiteHelper.getInstance(getContext()).getWritableDatabase();
        //getWritableDatabase(); //initialisere databasen som vi kan trenge.
        long id = 0; //Default error verdi.
        switch (URI_MATCHER.match(uri)) {
            case STATISTIKK_LIST: //Bruker denne for å legge til en rad.

                id = databaseWritable.insert(
                        StatisticsTable.TABLE_NAME,
                        null,
                        values);
                databaseWritable.close();

                if(id > 0){
                    if(PreferenceUtils.getSynchronizeAutomatically(getContext())){
                        scheduleSyncJob();
                    }
                }
                return getUriForId(id, uri);
            default:
                databaseWritable.close();
                throw new IllegalArgumentException("Unknown URI ? " + URI_MATCHER.match(uri));
        }
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] valuesList) {
        SQLiteDatabase db = SQLiteHelper.getInstance(getContext()).getWritableDatabase();

        switch (URI_MATCHER.match(uri)){
            case STATISTIKK_LIST: //bruker denne til å legge til flere rader sammtidig.
                int numberInserted = 0;
                db.beginTransaction();
                try {
                    //Inserter en value for hver av de.
                    for ( ContentValues value : valuesList ) {
                        if(db.insert(StatisticsTable.TABLE_NAME,
                                null,
                                value) == 0){
                            //Dersom verdien som returneres ved insert er større enn 0,
                                //så var det en vellykket insert settning.
                            throw new SQLException("Failed to insert "+ value.toString() + " into uri:" + uri);
                        }
                        //ellers så er det bare å forsette å inserte.
                    }
                    db.setTransactionSuccessful();
                    numberInserted = valuesList.length;
                } finally {

                    db.endTransaction();
                    db.close();

                }
                if(numberInserted > 0){ //sjekker at det ble lagt til minst en rad
                    if(PreferenceUtils.getSynchronizeAutomatically(getContext())){//sjekker at settigen er på.
                        scheduleSyncJob();//starter synching når vi har WIFI.
                    }
                }
                return numberInserted;
            default:
                db.close();
                throw new UnsupportedOperationException("unsupported uri: " + uri);
        }
    }


/*  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤JOB SCHEDULER¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *   JobScheduling av Syncing.
    * */

    public final static String synchJob = "sync_statistics_to_database_automatic";

    private void scheduleSyncJob(){
        // Create a new dispatcher using the Google Play driver.
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(getContext()));

        Job myJob = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(SynchJobService.class)

                // uniquely identifies the job
                .setTag(synchJob)

                // one-off job
                .setRecurring(false)

                // persist past a device reboot
                .setLifetime(Lifetime.FOREVER)

                // start between 0 and 120 seconds from now after constraints met.
                .setTrigger(Trigger.executionWindow(0, 120))

                // overwrite an existing job with the same tag
                .setReplaceCurrent(true)

                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)

                // constraints that need to be satisfied for the job to run
                .setConstraints(
                        // only run on an unmetered network, i vårt tilfelle Ikke mobilnett som koster penger.
                        Constraint.ON_UNMETERED_NETWORK
                        // only run when the device is charging
                        //Constraint.DEVICE_CHARGING
                )
                .build();

        dispatcher.mustSchedule(myJob);
    }

}
