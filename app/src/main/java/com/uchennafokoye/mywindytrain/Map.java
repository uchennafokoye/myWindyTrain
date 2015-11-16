package com.uchennafokoye.mywindytrain;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Handler;



public class Map extends Activity {

    ProgressDialog progressDialog;
    private int progressBarStatus = 0;
    private Handler progressBarHandler = new Handler();

    GoogleMap googleMap;
    GoogleDirection md = new GoogleDirection();
    private int progressValue = 0;

    double to_longitude;
    double to_latitude;
    String station_name;
    String trainsAtStation;
    String color;
    static Boolean httpRequested = false;


    Location current_location;
    Location starting_location;
    private LocationService locationService;
    private boolean bound = false;

    private Boolean firstTimeCameraMove = true;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        init();
    }

    private void init() {

        initializeProgressDialog();
        setUpMapIfNeeded();
        networkAvailabilityMessage();
//        attemptHttpAsyncTask();
        watchLocation();

    }

    private void initializeProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        progressDialog.setMessage("Loading Closest Location");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.setMax(100);
        progressDialog.show();

        progressBarStatus = 0;

        new Thread(new Runnable() {
            private int progressValueAnimatable = 0;

            public void run() {

                while (progressBarStatus < 100) {

                    if (progressValueAnimatable >= getProgressValue() && progressValueAnimatable < 99){
                        progressValueAnimatable += 5;
                    } else {
                        progressValueAnimatable = getProgressValue();
                    }

                    progressBarStatus = progressValueAnimatable;
                    Log.d("ProgressValue", progressBarStatus + "");

                    try {
                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    progressBarHandler.post(new Runnable() {
                        public void run() {
                            progressDialog.setProgress(progressBarStatus);
                        }
                    });


                }

                if (progressBarStatus >= 100){
                    try {
                        Thread.sleep(2000);

                    } catch(InterruptedException e){
                        e.printStackTrace();
                    }

                    progressDialog.dismiss();

                }

            }

        }).start();
    }

    public int getProgressValue() {

        if (googleMap == null){
            return 0;
        }

        if (locationService == null){
            return 10;
        }

        if (current_location == null){
            return 50;
        }

        if (!httpRequested) {
            return 70;
        }

        if (httpRequested & progressValue < 100) {
            return 99;
        }

        return 100;


    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (googleMap == null) {
            // Try to obtain the map from the GetFragmentManager
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        }
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(18));
    }


    //ATTEMPT TO CALL HTTP REQUEST
    private void attemptHttpAsyncTask() {

        if (current_location == null){
            return;
        }

        if (!httpRequested){

            String query_string = "/closest/" + current_location.getLatitude() + "/" + current_location.getLongitude();

            if (color != null) {
                query_string = "/" + color + query_string;
            }

            String url = "https://mwtservice.herokuapp.com" + query_string;
            Log.d("URL", url);
            new HttpAsyncTask().execute(url);

        }
    }



    private class HttpAsyncTask extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... urls){
            return GET(urls[0]);
        }

        @Override
        protected void onPostExecute(String result){

            try {
                JSONObject closest_station = new JSONObject(result);

                to_latitude = closest_station.getDouble("latitude");
                to_longitude = closest_station.getDouble("longitude");
                station_name = closest_station.getString("name");
                JSONArray listOfTrains = closest_station.getJSONArray("trains");
                trainsAtStation = "Trains At Station: " + listOfTrains.join(",");
                httpRequested = true;

                Toast toast = Toast.makeText(getBaseContext(), "Found closest train station! " + station_name, Toast.LENGTH_SHORT);
                toast.show();

                drawDirections();

                Log.d("current_location", current_location + "");

            } catch(JSONException e) {
                Log.d("JSON", e.getLocalizedMessage());
            }


        }
    }

    // LOCATION SERVICE

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder){
            LocationService.LocationBinder locationBinder = (LocationService.LocationBinder) binder;
            locationService = locationBinder.getLocationService();
            bound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName){
            bound = false;
        }
    };

    private void drawDirections() {

        googleMap.clear();


        if (current_location == null || !httpRequested) return;

        LatLng fromPosition = new LatLng(current_location.getLatitude(), current_location.getLongitude());
        drawCurrentLocation(fromPosition);

        LatLng toPosition = new LatLng(to_latitude, to_longitude);
        Log.d("toPosition", to_latitude + " " + to_longitude);
        googleMap.addMarker(new MarkerOptions().position(toPosition).title(station_name).snippet(trainsAtStation));

        Document doc = md.getDocument(fromPosition, toPosition, GoogleDirection.MODE_WALKING);
        drawPolyline(doc);
        updateMapBar(doc);
        progressValue = 100;

    }

    private void drawCurrentLocation(LatLng current_location) {


        googleMap.addMarker(new MarkerOptions().position(current_location).title("Current Position").flat(true).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        if (firstTimeCameraMove){
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(current_location));
            firstTimeCameraMove = false;
        }

    }


    private void drawPolyline(Document doc) {

        ArrayList<LatLng> directionPoint = md.getDirection(doc);
        PolylineOptions rectLine = new PolylineOptions().width(20).color(Color.RED);

        for (int i = 0; i < directionPoint.size(); i++){
            rectLine.add(directionPoint.get(i));
        }

        googleMap.addPolyline(rectLine);

    }

    private void updateMapBar(Document doc){


        TextView distancetv = (TextView) findViewById(R.id.tv_station_info_distance);
        TextView durationtv = (TextView) findViewById(R.id.tv_station_info_duration);
        TextView stationNametv = (TextView) findViewById(R.id.tv_station_info_name);

        distancetv.setText(md.getDistanceText(doc));
        durationtv.setText(md.getDurationText(doc));
        stationNametv.setText(station_name);
    }

    private void watchLocation() {
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (locationService != null) {

                    current_location = locationService.getLocation();
                    starting_location = (starting_location == null) ? current_location : starting_location;

                    Log.d("current_location", current_location + "");

                    if (!httpRequested){
                        attemptHttpAsyncTask();
                    }

                    if (locationService.locationChangedSinceLastChecked()){
                        if (locationService.distanceTraveled() > 2){
                            initializeProgressDialog();
                            httpRequested = false;
                            progressValue = 0;
                            attemptHttpAsyncTask();
                        } else {
                            drawDirections();
                        }

                    }

                }

                handler.postDelayed(this, 10000);
            }
        });
    }

    // END LOCATION SERVICE

    // CHECK INTERNET CONNECTION

    private void networkAvailabilityMessage() {
        if (!isConnected()){
            Toast toast = Toast.makeText(this, "No network available", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            Toast toast = Toast.makeText(this, "You are connected", Toast.LENGTH_SHORT);
            toast.show();
        }

    }
    private boolean isConnected() {

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()){
            return true;
        } else {
            return false;
        }


    }


    // GET  & HTTP HELPER METHODS
    public static String GET(String url){
        InputStream inputStream = null;
        String result = "";
        try {
            HttpClient httpclient = new DefaultHttpClient();

            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            inputStream = httpResponse.getEntity().getContent();


            if (inputStream != null){
                result = convertInputStreamToString(inputStream);
                Log.d("InputStream","In GET result: " + result + "inputStream =  " + inputStream);

            } else {
                result = "Did not work!";
            }




        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream){
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";

        Log.d("InputStream","In Input Stream" + inputStream);

        try {

            while ((line = bufferedReader.readLine()) != null){
                result += line;
            }
            inputStream.close();

        } catch(Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;

    }


    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, LocationService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



}
