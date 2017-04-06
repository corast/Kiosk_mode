package com.sondreweb.kiosk_mode_alpha.classes;

/**
 * Created by sondre on 06-Apr-17.
 */

public class GeofenceStatus {
    GeofenceClass geofence;
    boolean status;

    public GeofenceStatus(GeofenceClass geofence, boolean status){
        this.geofence = geofence;
        this.status = status;
    }

    public void setStatus(boolean status){
        this.status = status;
    }


}
