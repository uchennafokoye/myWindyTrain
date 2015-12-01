package com.uchennafokoye.mywindytrain;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import com.google.android.gms.maps.model.CameraPosition;
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
    public static final String SERVICE_ACCESS_TOKEN = "5RhswyZ4wqeThxrXZ47cYJKP";

    private boolean paused = false;

    ProgressDialog progressDialog;
    private int progressBarStatus;
    private int progressValue;
    private Handler progressBarHandler = new Handler();

    GoogleMap googleMap;
    GoogleDirection md = new GoogleDirection();
    MarkerOptions markerOptions = new MarkerOptions();

    private final Handler handler = new Handler();


    double to_longitude;
    double to_latitude;
    String station_name;
    String trainsAtStation;
    String color;
    static Boolean httpRequested;

    LocationService.customLocation current_location;
    LocationService.customLocation last_used_location;
    Marker cLMarker;


    private Boolean firstTimeCameraMove = true;

    Spinner colorSpinner;
    private CustomAdapterSpinner arrayAdapter;
    TextView tvTrainLine;

    Boolean dataConnected;

    HashMap<String, JSONObject> cachedJSONHash = new HashMap<>();


    SharedPreferences SP;
    boolean bDirUpdates;
    Double howOften;

    private LocationService locationService;
    private boolean bound = false;
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

    boolean mIsReceiverRegistered = false;
    NetworkChangeBR mReceiver = null;

    private class NetworkChangeBR extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent){

            dataConnected = isConnected();
            networkAvailabilityMessage();
            if (dataConnected){
                continueHttpRequests();
            }
            debugIntent(intent, "grokkingandroid");

        }

        private void debugIntent(Intent intent, String tag) {
            Log.v(tag, "action: " + intent.getAction());
            Log.v(tag, "component: " + intent.getComponent());
            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key: extras.keySet()) {
                    Log.v(tag, "key [" + key + "]: " +
                            extras.get(key));

                    Log.v(tag, extras.get(key).getClass().toString());
                }
            }
            else {
                Log.v(tag, "no extras");
            }
        }

    }

    public void goBack(View v) {
        v.startAnimation(MainActivity.buttonClick);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(SAVED_CURRENT_LOCATION, current_location);
        startActivity(intent);
    }

    public void expandSpinner(View v){
        v.startAnimation(MainActivity.buttonClick);
        colorSpinner.performClick();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        init();
    }

    private void init() {

        progressValue = 0;
        progressBarStatus = 0;
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading Closest Location");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);


        Intent intent = getIntent();
        color = intent.getStringExtra(COLORMESSAGE);
        current_location = (LocationService.customLocation) intent.getSerializableExtra(SAVED_CURRENT_LOCATION);

        tvTrainLine = (TextView) findViewById(R.id.tv_search_criteria_info);
        setColorTVText();

        /* Color Spinner */
        colorSpinner = (Spinner) findViewById(R.id.color_spinner);
        arrayAdapter = new CustomAdapterSpinner(this, android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(arrayAdapter);
        colorSpinner.setOnItemSelectedListener(this);

        httpRequested = false;


        setUpMapIfNeeded();


    }

    private void continueHttpRequests() {

        if (dataConnected){

            if (!httpRequested) {
                initializeProgressDialog();
                new HttpAsyncTask().execute();
            } else {
                drawDirections();
            }
        }

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
        progressDialog.setProgress(0);
        progressDialog.show();
        progressBarStatus = 0;

        new Thread(new Runnable() {
            private int progressValueAnimatable = 0;

            public void run() {

                while (progressBarStatus < 100 && !paused) {

                    int currentProgressValue = getProgressValue();
                    if (progressValueAnimatable >= currentProgressValue){
                        if ((progressValueAnimatable + 5) < nextProgressLevel(currentProgressValue)) {
                            progressValueAnimatable += 5;

                        }
                    } else {
                        progressValueAnimatable = getProgressValue();
                    }

                    progressBarStatus = progressValueAnimatable;

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
        updatePreferences();
        handler.post(watchLocation);
        paused = false;
        dataConnected = isConnected();
        networkAvailabilityMessage();

        if (dataConnected){
           continueHttpRequests();
        }


        /* BroadcastReceiver*/

        if (!mIsReceiverRegistered){
            if (mReceiver == null) mReceiver = new NetworkChangeBR();

            registerReceiver(mReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
            registerReceiver(mReceiver, new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED"));
            mIsReceiverRegistered = true;
        }


    }

    @Override
    protected void onPause () {
            super.onPause();
        handler.removeCallbacks(watchLocation);
        paused = true;

        if (mIsReceiverRegistered){
            unregisterReceiver(mReceiver);
            mReceiver = null;
            mIsReceiverRegistered = false;
        }
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




    private void setUpMapIfNeeded() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        }
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
            if (dataConnected){
                drawDirections();
            } else {
                noDataMessage();
            }
        }

        if (locationService.distanceTraveled(current_location, last_used_location) > 0.5) {
            httpRequested = false;
            if (dataConnected){
                progressValue = 0;
                initializeProgressDialog();
                new HttpAsyncTask().execute();
            } else {
                noDataMessage();
            }
        }

        setColorTVText();



    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class cachedMWTService extends AsyncTask<Void, Void, String>{
        @Override
        protected String doInBackground(Void... params){

            while (current_location == null){
                try { Thread.sleep(1000); }
                catch(InterruptedException e)
                { e.printStackTrace(); }
            }

            String query_string = "/closest/all/" + current_location.getLatitude() + "/" + current_location.getLongitude();
            query_string += "/" + SERVICE_ACCESS_TOKEN;
            String url = "https://mwtservice.herokuapp.com" + query_string;
            return GET(url);
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
            }
            catch(JSONException e) { e.printStackTrace();  }
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
            query_string += "/" + SERVICE_ACCESS_TOKEN;
            String url = "https://mwtservice.herokuapp.com" + query_string;
            return GET(url);
        }

        @Override
        protected void onPostExecute(String result){

            try {
                JSONObject closest_station = new JSONObject(result);

                parseJSON(closest_station);
                httpRequested = true;
                if (dataConnected){
                    drawDirections();
                } else {
                    noDataMessage();
                }


            } catch(JSONException e) {
                e.printStackTrace();
            }

            if (dataConnected){
                cachedMWTService task = new cachedMWTService();
                task.execute();
            }



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


    private void drawDirections() {

        googleMap.clear();
        if (current_location == null || !httpRequested) return;

        LatLng fromPosition = new LatLng(current_location.getLatitude(), current_location.getLongitude());
        drawCurrentLocation(fromPosition);

        LatLng toPosition = new LatLng(to_latitude, to_longitude);
        googleMap.addMarker(markerOptions.position(toPosition).title(station_name).snippet(trainsAtStation).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        Document doc = md.getDocument(fromPosition, toPosition, GoogleDirection.MODE_WALKING);
        drawPolyline(doc);
        updateMapBar(doc);
        progressValue = 100;

    }

    private void drawCurrentLocation(LatLng current_location) {

        if (cLMarker != null) {
            cLMarker.remove();
        }

        cLMarker = googleMap.addMarker(markerOptions.position(current_location).title("Current Position").snippet("").flat(true).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        if (firstTimeCameraMove){
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(current_location));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(current_location)      // Sets the center of the map to location user
                    .zoom(17)                   // Sets the zoom
                    .build();                   // Creates a CameraPosition from the builder
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
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

    Runnable watchLocation = new Runnable() {
            @Override
            public void run() {


                if (locationService != null) {

                    current_location = locationService.getLatLngLocation();
                    if (current_location != null) {

//                        if (!httpRequested) {
//                            googleMap.animateCamera(CameraUpdateFactory.newLatLng( new LatLng(current_location.getLatitude(), current_location.getLongitude())));
//                        }

                        if (bDirUpdates) {
                            Double asOften = (howOften <= 0.0 || howOften == null) ? 0.01 : howOften;
                            if (LocationService.distanceTraveled(current_location, last_used_location) >= asOften) {
                                if (dataConnected){
                                    drawDirections();
                                }
                            }
                        } else {

                            drawCurrentLocation(current_location);

                        }

                    }


                }

                if (!paused) handler.postDelayed(this, (current_location == null) ? 1000 : 10000);
            }
    };
    // END LOCATION SERVICE

    // CHECK INTERNET CONNECTION

    private void networkAvailabilityMessage() {
        if (!dataConnected){
            noDataMessage();
        }

    }

    private void noDataMessage() {
        Toast toast = Toast.makeText(this, "No data network available. Service paused temporarily.", Toast.LENGTH_SHORT);
        toast.show();
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

            } else {
                result = "Did not work!";
            }




        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream){
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        String result = "";

        try {

            while ((line = bufferedReader.readLine()) != null){
                result += line;
            }
            inputStream.close();

        } catch(Exception e) {
            e.printStackTrace();
        }

        return result;

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
