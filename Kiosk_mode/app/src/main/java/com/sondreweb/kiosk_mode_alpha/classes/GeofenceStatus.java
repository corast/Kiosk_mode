package com.sondreweb.kiosk_mode_alpha.classes;

import com.google.android.gms.location.Geofence;

/**
 * Holder på status til hvert av Geofencene.
 */

//Holder på et Geofence og en status. Som bare er Int verdien.
public class GeofenceStatus {
    Geofence geofence;
    boolean insideStatus = false; //Default status. Når de ikke er lagd er vi alltid utenfor.

    public GeofenceStatus(Geofence geofence){
        this.geofence = geofence;
    }

    public void setStatus(boolean status){
        this.insideStatus = status;
    }

    public void setStatus(int statusCode){
        if(statusCode == Geofence.GEOFENCE_TRANSITION_ENTER){
            insideStatus  = true;
        }else if(statusCode == Geofence.GEOFENCE_TRANSITION_EXIT){
            insideStatus = false;
        }else{//Bare DWELL igjenn, men den triggere vi ikke på uansett.
            insideStatus = false;
        }
    }

    public Geofence getGeofence(){
        return geofence;
    }

    public boolean getInsideStatus(){
        return insideStatus;
    }
}
