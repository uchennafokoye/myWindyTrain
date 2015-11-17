package com.uchennafokoye.mywindytrain;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends Activity implements View.OnClickListener{

    Location current_location;
    private LocationService locationService;
    private boolean bound = false;
    private boolean paused = false;

    SharedPreferences sharedPreferences;
    public static final String MyPREFERENCES = "MyPrefs";
    public static final String CURRENT_LATITUDE = "CurrentLocationLatitude";
    public static final String CURRENT_LONGITUDE = "CurrentLocationLongitude";





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {

        LinearLayout trainBlocks = (LinearLayout) findViewById(R.id.train_blocks);
        for (int i = 0; i < trainBlocks.getChildCount(); i++){
            View v = trainBlocks.getChildAt(i);
            v.setOnClickListener(this);
        }

        Button any_train = (Button) findViewById(R.id.any_train_button);
        any_train.setOnClickListener(this);

        sharedPreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        Log.d("SHAREDPREFERENCES", "CURRENT LATITUDE: " + sharedPreferences.getFloat(MainActivity.CURRENT_LATITUDE, (float) 0.0));
        Log.d("SHAREDPREFERENCES", "CURRENT LONGITUDE: " + sharedPreferences.getFloat(MainActivity.CURRENT_LONGITUDE, (float) 0.0));

        watchLocation();


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

    private void watchLocation() {
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (locationService != null) {

                    current_location = locationService.getLocation();



                    Log.d("current_location", current_location + "");

                }

                if (!paused) handler.postDelayed(this, 10000);
            }
        });
    }

    @Override
    public void onClick(View v){


        String color = (String) v.getTag();
        color = (color.equals("any")) ? null : color;
        Intent intent = new Intent(this, Map.class);
        if (color != null){
            intent.putExtra(Map.COLORMESSAGE, color);
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();

        Float savedLatitude;
        Float savedLongitude;

        if (current_location != null){
            savedLatitude = (float) current_location.getLatitude();
            savedLongitude = (float) current_location.getLongitude();
        } else {
            savedLatitude = (float) 0.0;
            savedLongitude = (float) 0.0;
        }

        editor.putFloat(CURRENT_LATITUDE, savedLatitude);
        editor.putFloat(CURRENT_LONGITUDE, savedLongitude);
        editor.commit();



        startActivity(intent);

    }


    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, LocationService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        paused = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
        paused = true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
