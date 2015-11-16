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


public class LocationService extends Service {

    private final IBinder binder = new LocationBinder();
    private static Location location = null;
    private static Location last_location = null;
    private static Location last_location_since_last_checked = null;


    public LocationService() {
    }

    @Override
    public void onCreate() {
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location current_location){
                last_location = location;
                location = current_location;
                Log.d("PermissionGPS", "Location Changed " + location);

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

            Long min_sec = (long) 5000;
            Float min_distance = (float) 1.0;

            Criteria criteria = new Criteria();
            String bestProvider = locManager.getBestProvider(criteria, true);
            location = locManager.getLastKnownLocation(bestProvider);
            locManager.requestLocationUpdates(bestProvider, min_sec, min_distance, listener);

            Log.d("current_location", location + "");
            Log.d("PermissionGPS", "Security Exception None");

        } catch (SecurityException e){
            Log.d("PermissionGPS", "Security Exception");
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

    public Boolean locationChangedSinceLastChecked() {

        Boolean location_changed = location != last_location_since_last_checked;

        //reset last_location_since_last_checked to be current location
        last_location_since_last_checked = location;

        return location_changed;
    }


    public Float distanceTraveled() {
        if (last_location == null){
            Log.d("Distance traveled", 0.0 + "");
            return (float) 0.0;
        } else {
            Log.d("Distance traveled", location.distanceTo(last_location) + " ");
            return location.distanceTo(last_location);

        }
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
}
