package com.sondreweb.kiosk_mode_alpha.classes;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.sondreweb.kiosk_mode_alpha.R;

/**
 * Created by sondre on 23-Mar-17.
 */

//TODO: fiks onclick listenern, slik at vi kan sende brukeren til riktig sted i settings nør det er nødvendig.

public class StatusInfo {

    //Status, true vill si klar, false vill si ikke klar(må gjøre noe med det da i så fall.
    private boolean status = false;

    //Navn på statusen
    private String name;

    //Holder på infoen til statusen.
    private String info = null;

    //Holder på hva slags icon som skal vises på statusen.
    private String imageName = null;

    //Konstruktør
    public StatusInfo(String name){
        info = null;
        imageName = null;
        this.name = name;
    }

    /*
    *   Set
    * */
    public void setInfo(String info){
        this.info = info;
    }

    public void setStatus(boolean status){
        this.status = status;
    }

    public void setImageName(String imageName){
        this.imageName = imageName;
    }

    /*
    *   Get
    * */
    public boolean getStatus(){
        return status;
    }

    public String getName(){
        return this.name;
    }

    public String getInfo(){
        if(info != null){
            return info;
        }else
            return "No info found";

    }

    //returnerer Image resource value.
    public int getImageDrawable(Context context) {
        if (imageName == null) {
            try {
                return R.drawable.common_google_signin_btn_icon_dark;
            } catch (NullPointerException e) {
                return R.drawable.visible_50;
            }
        } else {
            return context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());
        }

    }

    @Override
    public String toString() {
        return name + " status: " + status;
    }
}
