package com.sondreweb.kiosk_mode_alpha.storage;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.sondreweb.kiosk_mode_alpha.jobscheduler.CustomJobService;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sondre on 10-Apr-17.
 * Skal ha ansvar for å lagre data til en database eller hva nå enn vi skal lagrde.
 * Denne skal brukes for å lagre statistikken vi har
 */

public class CustomContentProvider extends ContentProvider {

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
    private static final int STATISTIKK_ID = 1;

    private static final int STATISTIKK_LIST = 2;


        //Vi har allerede dette i en annen fil.
    static final String id = "id";
    static final String name = "name";

    static final int uriCode = 1; //kode som vi bruker til et eller annet.

    //content://authority/optionalPath/optionalId
    static final String URL = "content://" + PROVIDER_NAME + "/"+StatisticsTable.TABLE_NAME;

    static final Uri CONTENT_URL = Uri.parse(URL); //Hva brukes denne til TODO: finn ut hva CONTENT_URI brukes til


    private static HashMap<String, String> values; //TODO: Funn ut hva dette faktisk er(hashmap).
    /*
    *   Tror dette er verdiene vi skal sette inn, vi har ikke bare Stringer men..
    * */
    //lager root noden for URI treet.

    static final UriMatcher URI_MATCHER;
    static{
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(
                PROVIDER_NAME,
                "statistikk",
                STATISTIKK_ID
        );

        URI_MATCHER.addURI(
                PROVIDER_NAME,
                "statistikk/#",//# vill si flere
                STATISTIKK_LIST
        );

        URI_MATCHER.addURI(StatisticsItemsContract.AUTHORITY,
                "statistikk",+
                        STATISTIKK_ID);
    }

    private SQLiteHelper sqLiteHelper = null;
    private SQLiteDatabase sqLiteDatabaseW = null;
    private SQLiteDatabase sqLiteDatabaseR = null;

    private final ThreadLocal<Boolean> mIsInBatchMode = new ThreadLocal<Boolean>();

    /*
    *   Prepares the content provider
    *   return:  true if the provider was successfully loaded, false otherwise
    * */
    @Override
    public boolean onCreate() {
        //initialisere databasen vår.
        sqLiteHelper = SQLiteHelper.getInstance(getContext()); //lager databasen dersom det ikke er gjordt.
        sqLiteDatabaseW = sqLiteHelper.getWritableDatabase();
        sqLiteDatabaseR = sqLiteHelper.getReadableDatabase();
        /*
        *   A content provider is created when its hosting process is created,
        *   and remains around for as long as the process does, so there is no need to close the database --
        *   it will get closed as part of the kernel cleaning up the process's resources when the process is killed.
        * */
        return true;
    }

    @Override
    public void onLowMemory() {
        //Når vi har lite med minne, hva gjør vi da?
        /*
        *   Ved lite minne, må vi be brukeren gå tilbake til Resepsjonen og bytte ut enheten, siden denne må restartet slik at Cashe kan cleares.
        * */
        super.onLowMemory();
    }

    /*
    *   Returns the MIME type for this URI
    * */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (URI_MATCHER.match(uri)){
            case STATISTIKK_LIST ://dersom vi skal ha tak i en eller flere rader i tabellen.
                /*  CURSOR_DIR_BASE_TYPE= vnd.android.cursor.dir
                *   "vnd.android.cursor.dir/com.sondreweb.kiosk_mode_alpha.storage.statisticsItems"
                *    "vnd.android.cursor.dir/vnd.com.androidcontentproviderdemo.androidcontentprovider.provider.images";*/
                return StatisticsItems.CONTENT_TYPE;
            case STATISTIKK_ID: //Dersom vi skal ha tak i en rad
                /*  CURSOR_ITEM_BASE_TYPE= vnd.android.cursor.item
                *   "vnd.android.cursor.item/com.sondreweb.kiosk_mode_alpha.storage.statisticsItems
                * */
                return StatisticsItems.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported URI "+uri);
        }
    }

    /*
     *   Adds records
     *   values kan komme inn som et Statistik objekt kanskje?
     * */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {

        switch (URI_MATCHER.match(uri)) {
            case STATISTIKK_ID:
                long id = sqLiteDatabaseW.insert(
                        StatisticsTable.TABLE_NAME,
                        null,
                        values);
                return getUriForId(id, uri);
            case STATISTIKK_LIST:
                break;
            default:
                throw new IllegalArgumentException("Unknown URI");
        }
        return uri;
    }



    /*  GAMMEL KODE, MEN ER SÅ FIN.
    *     if(URI_MATCHER.match(uri) != STATISTIKK_ID){
            throw new IllegalArgumentException(
                    "Unsupported URI for insertion: "+uri);
        }

        SQLiteDatabase db = sqLiteHelper.getWritableDatabase();
        switch (URI_MATCHER.match(uri)){
            case STATISTIKK_LIST:
                long id = db.insert(
                        StatisticsTable.TABLE_NAME,
                        null,
                        values);
                return getUriForId(id,uri);
        }
        if(URI_MATCHER.match(uri) == STATISTIKK_ID){ //Legge til en rad.
            long id = db.insert(
                    StatisticsTable.TABLE_NAME,
                    null,
                    values);
            return getUriForId(id, uri);
        }

        return null;
    * */


    //Hva gjør denne?

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

        throw new SQLException(
                "problem while inserting into uri: "+uri);
    }

    /*
        *   Modifies data
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

        String id = null;
        if(URI_MATCHER.match(uri) == STATISTIKK_ID){
            id = uri.getPathSegments().get(1);
        }
        return sqLiteHelper.getStatistics(id,projection,selection,selectionArgs,sortOrder);
    }


    /*
    *   Deletes records
    * */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }


    /*
    *   For å legge til flere enn en value/rad. Men aner ikke hvordan operations fungerer.
    * */
    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(
            @NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabase db = sqLiteHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            final ContentProviderResult[] returnResult = super.applyBatch(operations);
            db.setTransactionSuccessful();
            getContext().getContentResolver().notifyChange(StatisticsItemsContract.CONTENT_URI, null);

            return returnResult;
        }
        finally {
            mIsInBatchMode.remove();
            db.endTransaction();
        }
    }

    private boolean isInBatchMode() {
        return mIsInBatchMode.get() != null && mIsInBatchMode.get();
    }



    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        SQLiteDatabase db = sqLiteHelper.getWritableDatabase();


        switch (URI_MATCHER.match(uri)){
            case STATISTIKK_LIST:
                int numberInserted = 0;
                db.beginTransaction();
                try {
                    //Inserter en value for hver av de.
                    for (ContentValues value : values ) {
                        db.insert(StatisticsTable.TABLE_NAME,
                                null,
                                value);
                    }
                    db.setTransactionSuccessful();
                    numberInserted = values.length;
                } finally {
                    db.endTransaction();
                }
                return numberInserted;

            default:
                throw new UnsupportedOperationException("unsupported uri: " + uri);
        }
    }

/*  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤JOB SCHEDULER¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *   JobScheduling av Syncing.
    * */

    public final static String syncKey = "com.firebase.sync.key";
    private final static String syncValue = "com.firebase.sync.value";
    public final static String jobTag = "SYNC_WITH_DATABASE";


    private void scheduleSyncJob(){
        // Create a new dispatcher using the Google Play driver.
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(getContext()));

        Bundle myExtrasBundle = new Bundle();

        myExtrasBundle.putString(syncKey,syncValue);

        Job myJob = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(CustomJobService.class)
                // uniquely identifies the job
                .setTag(jobTag)
                // one-off job
                .setRecurring(false)
                // don't persist past a device reboot
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                // start between 0 and 60 seconds from now
                .setTrigger(Trigger.executionWindow(0, 60))
                // overwrite an existing job with the same tag
                .setReplaceCurrent(true)
                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                // constraints that need to be satisfied for the job to run
                .setConstraints(
                        // only run on an unmetered network, i vårt tilfelle Ikke mobilnett som koster penger.
                        Constraint.ON_UNMETERED_NETWORK,
                        // only run when the device is charging
                        Constraint.DEVICE_CHARGING
                )
                .setExtras(myExtrasBundle)
                .build();

        dispatcher.mustSchedule(myJob);
        dispatcher.schedule(myJob);
    }

}
