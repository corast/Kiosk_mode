package com.sondreweb.kiosk_mode_alpha.classes;

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

    String id; //ID to diffentiate the geofences.
    double latitude; //Placement in Lat
    double longitude; //Placement in lon
    float radius; //Radius of the GeofenceClass.

    public GeofenceClass(String id, double latitude, double longitude, float radius){
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }


}
