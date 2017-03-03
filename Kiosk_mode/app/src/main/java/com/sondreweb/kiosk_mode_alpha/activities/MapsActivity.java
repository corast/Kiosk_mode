package com.sondreweb.kiosk_mode_alpha.activities;

import android.content.Context;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sondreweb.kiosk_mode_alpha.R;


/**
 * Created by sondre on 03-Mar-17.
 *
 * Denne har bare ansvar for å vise et kart og vår lokasjon på kartet kontinuelig/Denne må da altså ha tak i Location.
 */

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback{
    private static final String TAG = MapsActivity.class.getSimpleName();

    private MapFragment mapFragment;
    private GoogleMap gMap;


    private TextView zoom_tv,gps_lat,gps_long;
    private SeekBar seekBar_zoom;
    private float zoom = 12;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);
        //googleMap = new GoogleMap();

        gps_lat = (TextView) findViewById(R.id.tv_Lat);
        gps_long = (TextView) findViewById(R.id.tv_Long);

        zoom_tv = (TextView) findViewById(R.id.tv_zoom);
        zoom_tv.setText("zoom 12");

        seekBar_zoom = (SeekBar) findViewById(R.id.seekBar_zoom);
        seekBar_zoom.setMax(9);
        seekBar_zoom.setProgress(0);
        seekBar_zoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //progress fra 0-9 i dette tilfelle.
                /*  Zoom fra 12 - 21 for oss. 21 vill si at vi ser bygninger.
                 *      21/(21-12) = 9 nivåer
                 *      slik at 0 blir 12 og 21 blir 21.
                 **/
                zoom = progress + 12; //0->12 og 9->21
                zoom_tv.setText("zoom "+zoom);
                cameraUpdate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });

        //Layoout instansiert.

        if(isGooglePlayServicesAvaliable()){
            googlePlayServicesIsAvalible();
        }

    }

    private boolean isGooglePlayServicesAvaliable(){
        Log.d(TAG,"isGooglePlayServicesAvaliable");
        GoogleApiAvailability googleApiAvailbility = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailbility.isGooglePlayServicesAvailable(this);
        if( resultCode != ConnectionResult.SUCCESS){
            //gir brukeren beskjed om hvorfor det ikke gikk
            try {
                if (googleApiAvailbility.isUserResolvableError(resultCode)) {
                    googleApiAvailbility.getErrorDialog(this, resultCode, 2404).show();
                }
            }catch (Exception e) {
                Log.e("Error"+TAG," "+e);
            }
        }
        return resultCode == ConnectionResult.SUCCESS;
    }

    public void googlePlayServicesIsAvalible(){
        Log.d(TAG,"isGooglePlayServiceAvalible: true");
        LocationManager manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        //sjekker om GPS og Nettverk er påskrudd i enheten. WIfi kan være avskrudd, men ikke mobilnett.
        if( ! manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && ! manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            Toast.makeText(this, "Enable location services for accurate data", Toast.LENGTH_SHORT).show();
        }else
        {
            Log.d(TAG, "Location Enabled");
        }

        initGoogleMaps(); //initalize Google maps fragment to show map and our location.
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
       // createGoogleApi();
    }


    //Initalize GoogleMaps
    private void initGoogleMaps() {
        Log.d(TAG,"initGoogleMaps()");
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);//getMapAsyncronous, get map from another thread as fast as possible, no delay for UI thread this way.
        //callback to main thread, i.e MainActivity callback.
    }

   /* //Create GoogleApiClient Instance
    private void createGoogleApi() {
        Log.d(TAG, "createGoogleApi()");
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            Log.d(TAG,googleApiClient.toString());
        }
    }
    */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        gMap.setMinZoomPreference(zoom);
    }

    private CameraUpdate cameraUpdate;
    private Marker locationMarker;

    //Rød marker, som vi zoomer til(som er vår posisjon.)
    private void markerLocation(LatLng latLng) {
        Log.d(TAG,"markerLocation()");
        //Log.i(TAG, "markerLocation(" + latLng + ")");
        String title = latLng.latitude + ", " + latLng.longitude;
        //optioner for markeren vi bruker. Gir den tittelen som kordinater og posisjonen.
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title);
        if (gMap != null) {   //Remove the anterior marker
            if (locationMarker != null) {
                locationMarker.remove();
            }
            locationMarker = gMap.addMarker(markerOptions);

            cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            gMap.animateCamera(cameraUpdate);
        }
    }

    private void cameraUpdate(){
        if(locationMarker != null) {
            cameraUpdate = CameraUpdateFactory.newLatLngZoom(locationMarker.getPosition(), zoom);
            gMap.animateCamera(cameraUpdate);
        }
    }
}
