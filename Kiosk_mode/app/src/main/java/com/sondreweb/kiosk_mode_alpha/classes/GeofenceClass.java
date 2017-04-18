package com.sondreweb.kiosk_mode_alpha.classes;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by sondre on 30-Mar-17.
 *
 * Tror ikke det er behov for denne
 */

public class GeofenceClass {

    //TODO Lag Objecter som holder på kordinater osv til et GeofenceClass, slik at vi enkelt kan lage flere.

    //Class for creating geoLocation objects, which will be responsible for holding information about Geofences which we might need.

    private static List<GeofenceClass> geofenceList;

    String requestId; //ID to diffentiate the geofences.
    double latitude; //Placement in Lat
    double longitude; //Placement in lon
    float radius; //Radius of the GeofenceClass.

    public GeofenceClass(String requestId, double latitude, double longitude, float radius){
        this.requestId = requestId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    //For når vi skal legge geofence til database, da trenger vi ikke bry oss om annet en LatLng og radius, siden det lages en request id ved insetting.
    public GeofenceClass(double latitude, double longitude, float radius){
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    public String getRequestId(){
        return requestId;
    }

    public LatLng getLatLng(){
        //lager et LatLng object av dette.
       return new LatLng(latitude, longitude);
    }

    public float getRadius(){
        return radius;
    }

    @Override
    public String toString() {
        return requestId + " cordinates: "+ latitude+", "+longitude+"  rad:"+radius+"m";
    }
}
