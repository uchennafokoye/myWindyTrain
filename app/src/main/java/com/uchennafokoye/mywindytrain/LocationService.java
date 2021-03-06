package com.uchennafokoye.mywindytrain;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.util.Log;

import java.io.Serializable;


public class LocationService extends Service {

    private final IBinder binder = new LocationBinder();
    private static Location location = null;
    private static Location last_location_since_last_checked = null;
    private static customLocation custom_location = null;


    public LocationService() {
    }

    @Override
    public void onCreate() {
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location current_location){
                location = current_location;
                custom_location = new customLocation(current_location.getLatitude(), current_location.getLongitude());

            }

            @Override
            public void onProviderDisabled(String arg0){}

            @Override
            public void onProviderEnabled(String arg0){}

            @Override
            public void onStatusChanged(String ar0, int arg1, Bundle bundle) {}


        };



        LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {

            Long min_sec = (long) 1000;
            Float min_distance = (float) 1.0;
//
//            Criteria criteria = new Criteria();
//            String bestProvider = locManager.getBestProvider(criteria, true);
//            location = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//            if (location != null){
//                custom_location = new customLocation(location.getLatitude(), location.getLongitude());
//            }
            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, min_sec, min_distance, listener);

        } catch (SecurityException e){
            e.printStackTrace();
        }
    }

    public double getLatitude() {
        return location.getLatitude();
    }

    public double getLongitude() {
        return location.getLongitude();
    }

    public Location getLocation() {
        return LocationService.location;
    }

    public customLocation getLatLngLocation() { return custom_location; }

    public Boolean locationChangedSinceLastChecked() {

        Boolean location_changed = location != last_location_since_last_checked;

        //reset last_location_since_last_checked to be current location
        last_location_since_last_checked = location;

        return location_changed;
    }

    public static Float distanceTraveled(customLocation current_location, customLocation last_location) {

        if (current_location == null || last_location == null) { return (float) 0.0; }

        Location mcLocation = new Location(location);
        mcLocation.setLongitude(current_location.getLongitude());
        mcLocation.setLatitude(current_location.getLatitude());

        Location mlLocation = new Location(location);
        mlLocation.setLongitude(last_location.getLongitude());
        mlLocation.setLatitude(last_location.getLatitude());

        return (mcLocation.distanceTo(mlLocation) / 1000);

    }
    public class LocationBinder extends Binder {

        LocationService getLocationService() {
            return LocationService.this;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    public static class customLocation implements Serializable {
        private final double latitude;
        private final double longitude;

        public customLocation(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }


        public double getLatitude() {
            return this.latitude;
        }

        public double getLongitude(){
            return this.longitude;
        }

        @Override
        public String toString() {
            return this.latitude + " " + this.longitude;
        }


    }
}
