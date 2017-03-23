package com.sondreweb.kiosk_mode_alpha;

import android.graphics.drawable.Drawable;

/**
 * Created by sondre on 23-Mar-17.
 */

public class StatusInfo {

    //Status, true vill si klar, false vill si ikke klar(må gjøre noe med det da i så fall.
    private boolean status = false;

    //Navn på statusen
    private String name;

    //Hvor vi kan sende brukeren for å skru på dette, dersom det er mulig.
    private String settingInfo;
    
    public StatusInfo(String name){
        this.name = name;
    }

    public boolean getStatus(){
        return status;
    }

    public String getName(){
        return this.name;
    }

    public void setStatus(boolean status){
            this.status = status;
    }

    //returnerer Image resource value.
    public int getImageDrawable(){
        try {
            return R.drawable.common_google_signin_btn_icon_dark;
        }catch (NullPointerException e){
            return R.drawable.visible_50;
        }
    }

    /**
     *
     * Basert på hva det er, så skal denne oppdatere statusen. Vi kan se alle statuser fra AppUtils.
     */

    public boolean updateStatus(){
        return false;
    }



    @Override
    public String toString() {
        return name + " status: " + status;
    }
}
