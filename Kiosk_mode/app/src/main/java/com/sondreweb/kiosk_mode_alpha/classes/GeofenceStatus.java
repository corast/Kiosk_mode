package com.sondreweb.kiosk_mode_alpha.classes;

import com.google.android.gms.location.Geofence;

/**
 * Created by sondre on 06-Apr-17.
 * Trenger ikke denne!
 */

//Holder på et Geofence og en status. Som bare er Int verdien.
public class GeofenceStatus {
    Geofence geofence;
    boolean insideStatus = true; //Default status. Vi setter default true, pga første gang, kan det hende at vi triggere på exit trigger.
        //Og pga det så kan det hende at det slås alarm på at vi er utenfor alle geofenfence.
    //TODO: test ut dette og det gamle som var false.

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
