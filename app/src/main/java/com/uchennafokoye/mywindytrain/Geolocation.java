package com.uchennafokoye.mywindytrain;

/**
 * Created by faithfulokoye on 10/29/15.
 */
public class Geolocation {

    private double latitude;
    private double longitude;

    public Geolocation(double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }


    @Override
    public String toString() {
        return "Langitude: " + Double.toString(latitude) + " Longitude: " + Double.toString(longitude);
    }
}
