package com.uchennafokoye.mywindytrain;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
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
import java.util.HashMap;

import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Handler;



public class Map extends Activity implements AdapterView.OnItemSelectedListener {


    public static final String COLORMESSAGE = "message";
    public static final String SAVED_CURRENT_LOCATION = "saved_current_location";

    private boolean paused = false;

    ProgressDialog progressDialog;
    private int progressBarStatus;
    private Handler progressBarHandler = new Handler();

    GoogleMap googleMap;
    GoogleDirection md = new GoogleDirection();
    private int progressValue;

    double to_longitude;
    double to_latitude;
    String station_name;
    String trainsAtStation;
    String color;
    static Boolean httpRequested;

    LocationService.customLocation current_location;
    LocationService.customLocation last_used_location;
    Marker cLMarker;

    private LocationService locationService;
    private boolean bound = false;


    private Boolean firstTimeCameraMove = true;

    Spinner colorSpinner;
    private CustomAdapterSpinner arrayAdapter;

    TextView tvTrainLine;

    HashMap<String, JSONObject> cachedJSONHash = new HashMap<>();


    SharedPreferences SP;
    boolean bDirUpdates;
    Double howOften;

    public void goBack(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(SAVED_CURRENT_LOCATION, current_location);
        startActivity(intent);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        Log.d("ONCREATE", "IN ON CREATE");
        init();
    }

    private void init() {

        updatePreferences();

        resetMapApp();
        Intent intent = getIntent();
        color = intent.getStringExtra(COLORMESSAGE);
        current_location = (LocationService.customLocation) intent.getSerializableExtra(SAVED_CURRENT_LOCATION);
        Log.d("INTENT_GET", current_location + "");

        tvTrainLine = (TextView) findViewById(R.id.tv_search_criteria_info);
        setColorTVText();

        /* Color Spinner */
        colorSpinner = (Spinner) findViewById(R.id.color_spinner);
        arrayAdapter = new CustomAdapterSpinner(this, android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(arrayAdapter);
        colorSpinner.setOnItemSelectedListener(this);


        httpRequested = false;

        initializeProgressDialog();
        setUpMapIfNeeded();
        networkAvailabilityMessage();
        new HttpAsyncTask().execute();
        watchLocation();

    }


    private void setColorTVText() {
        if (color != null){
            tvTrainLine.setText(Character.toUpperCase(color.charAt(0)) + color.substring(1) + " Line" );
            TrainColor mycolor = new TrainColor(color);
            tvTrainLine.setTextColor(Color.parseColor(mycolor.color));
        } else {
            tvTrainLine.setText("Any Line");
            tvTrainLine.setTextColor(Color.WHITE);
        }
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

                    int currentProgressValue = getProgressValue();
                    if (progressValueAnimatable >= currentProgressValue){
                        if ((progressValueAnimatable + 5) < nextProgressLevel(currentProgressValue)) {
                            progressValueAnimatable += 5;

                        }
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

            public int nextProgressLevel(int progressValue) {

                int nextProgressLevel;
                switch (progressValue){
                    case 0:
                        nextProgressLevel = 10;
                        break;
                    case 10:
                        nextProgressLevel = 50;
                        break;
                    case 50:
                        nextProgressLevel = 70;
                        break;
                    case 70:
                        nextProgressLevel = 99;
                        break;
                    case 99:
                        nextProgressLevel = 100;
                        break;
                    default:
                        nextProgressLevel = 100;
                        break;
                }

                return nextProgressLevel;
            }

        }).start();
    }


    private void updatePreferences() {

        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        bDirUpdates = SP.getBoolean("directionUpdates", false);
        String hOften = SP.getString("howOften", "0.5");

        try {
            howOften = Double.parseDouble(hOften);
        } catch(NumberFormatException e){
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("ONCREATE", "IN ON RESUME");

        updatePreferences();
        setUpMapIfNeeded();
        resetMapApp();
        paused = false;
    }

    @Override
    protected void onPause() {
        Log.d("ONCREATE", "IN ON PAUSE");
        super.onPause();
        resetMapApp();
        paused = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, LocationService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        paused = false;
        Log.d("ONCREATE", "IN ON START");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("ONCREATE", "IN ON STOP");

        if (bound) {
            unbindService(connection);
            bound = false;
        }
        resetMapApp();
        paused = true;
    }




    private void setUpMapIfNeeded() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        }
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(18));
    }




    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        String[] colorArray = TrainColor.TRAIN_COLORS_ARRAY;
        if (position < 0 || position >= colorArray.length) {
            return;
        }

        if (!httpRequested || current_location == null) { return; }


        String trainColor = colorArray[position];
        color = (trainColor.equals("Any")) ? null : trainColor;
        firstTimeCameraMove = true;

        if (cachedJSONHash.size() != 0) {
            JSONObject closest_station = cachedJSONHash.get(trainColor);
            parseJSON(closest_station);
            drawDirections();
        }

        if (locationService.distanceTraveled(current_location, last_used_location) > 0.5) {
            httpRequested = false;
            progressValue = 0;
            initializeProgressDialog();
            new HttpAsyncTask().execute();
        }

        setColorTVText();



    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class cachedMWTService extends AsyncTask<Void, Void, String>{
        @Override
        protected String doInBackground(Void... params){
            Log.d("Thread_ID", "Thread name: " + Thread.currentThread().getName() + " Thread ID;" + Thread.currentThread().getId());

            while (current_location == null){
                try { Thread.sleep(1000); }
                catch(InterruptedException e)
                { e.printStackTrace(); }
            }

            String query_string = "/closest/all/" + current_location.getLatitude() + "/" + current_location.getLongitude();
            String url = "https://mwtservice.herokuapp.com" + query_string;
            Log.d("URL", url); return GET(url);
        }

        @Override
        protected void onPostExecute(String result){
            try {
                JSONObject closest_stations = new JSONObject(result);
                String[] trainColorArray = TrainColor.TRAIN_COLORS_ARRAY;
                for (String trainColor : trainColorArray) {
                    JSONObject cachedJSON = closest_stations.getJSONObject(trainColor);

                    cachedJSONHash.put(trainColor, cachedJSON);
                }
                Log.d("CACHEDJSON", cachedJSONHash.toString());
            }
            catch(JSONException e) { Log.d("JSON", e.getLocalizedMessage()); }
        }
    }


    private class HttpAsyncTask extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... colors){

            while (current_location == null){
                try {
                    Thread.sleep(1000);

                } catch(InterruptedException e){
                    e.printStackTrace();
                }

            }

            last_used_location = current_location;
            String query_string = "/closest/" + current_location.getLatitude() + "/" + current_location.getLongitude();

            if (color != null) {
                query_string += "/" + color;
            }

            String url = "https://mwtservice.herokuapp.com" + query_string;
            Log.d("URL", url);
            return GET(url);
        }

        @Override
        protected void onPostExecute(String result){

            try {
                JSONObject closest_station = new JSONObject(result);

                parseJSON(closest_station);
                httpRequested = true;

                drawDirections();

                Log.d("current_location", current_location + "");

            } catch(JSONException e) {
                Log.d("JSON", e.getLocalizedMessage());
            }

            cachedMWTService task = new cachedMWTService();
            task.execute();


        }
    }

    // PARSE JSON
    private void parseJSON(JSONObject closest_station) {

        try {

            to_latitude = closest_station.getDouble("latitude");
            to_longitude = closest_station.getDouble("longitude");
            station_name = closest_station.getString("name");
            JSONArray listOfTrains = closest_station.getJSONArray("trains");
            trainsAtStation = "Trains At Station: " + listOfTrains.join(",");
        }
        catch (JSONException e) { e.printStackTrace();
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

        if (cLMarker != null) {
            cLMarker.remove();
        }

        cLMarker = googleMap.addMarker(new MarkerOptions().position(current_location).title("Current Position").flat(true).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        if (firstTimeCameraMove){
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(current_location));
            firstTimeCameraMove = false;
        }

    }

    private void drawCurrentLocation(LocationService.customLocation current_location){
        if (current_location == null) { return; }
        drawCurrentLocation(new LatLng(current_location.getLatitude(), current_location.getLongitude()));
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

                    while (locationService.getLatLngLocation() == null){
                        Log.d("WATCH_LOCATION", "Location is null");

                        try {
                            Thread.sleep(1000);
                        } catch(InterruptedException e){
                            Log.d("WATCH_LOCATION", "Sleep interrupted");
                        }

                    }


                    // To make sure we get accurate location
                    try {
                        Thread.sleep(3000);
                    } catch(InterruptedException e){
                        Log.d("WATCH_LOCATION", "Sleep interrupted");
                    }

                    current_location = locationService.getLatLngLocation();



                    if (bDirUpdates){
                        Log.d("UPDATE DIRECTIONS", "true");
                        Double asOften = (howOften <= 0.0 || howOften == null) ? 0.5 : howOften;
                        Log.d("howOften", Double.toString(asOften));
                        if (LocationService.distanceTraveled(current_location, last_used_location) >= asOften){
                            drawDirections();
                        }
                    } else {
                        Log.d("UPDATE DIRECTIONS", "false");

                    }


                }

                if (!paused) handler.postDelayed(this, 10000);
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
        return networkInfo != null && networkInfo.isConnected();


    }


    // GET  & HTTP HELPER METHODS
    public static String GET(String url){
        InputStream inputStream;
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
        String line;
        String result = "";

        Log.d("InputStream", "In Input Stream" + inputStream);

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





    private void resetMapApp() {
        progressValue = 0;
        progressBarStatus = 0;
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
