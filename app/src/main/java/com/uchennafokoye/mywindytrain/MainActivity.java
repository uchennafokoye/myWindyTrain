package com.uchennafokoye.mywindytrain;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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

    LocationService.customLocation current_location;
    private LocationService locationService;
    private boolean bound = false;
    private boolean paused = false;


    ProgressDialog progressDialog;
    private int progressBarStatus;
    private Handler progressBarHandler = new Handler();

    final Handler handler = new Handler();






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, LocationService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        init();
    }

    private void init() {

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Getting Location");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMax(100);

        LinearLayout trainBlocks = (LinearLayout) findViewById(R.id.train_blocks);
        for (int i = 0; i < trainBlocks.getChildCount(); i++) {
            View v = trainBlocks.getChildAt(i);
            v.setOnClickListener(this);
        }

        Button any_train = (Button) findViewById(R.id.any_train_button);
        any_train.setOnClickListener(this);

        Intent intent = getIntent();
        current_location = (LocationService.customLocation) intent.getSerializableExtra(Map.SAVED_CURRENT_LOCATION);
        Log.d("FROM_INTENT_MAIN", current_location + "");

//        initializeProgressDialog();

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

    Runnable watchLocation = new Runnable() {
            @Override
            public void run() {
                if (locationService != null) {

                    current_location = locationService.getLatLngLocation();
                    Log.d("current_location", current_location + "");

                }

                if (!paused) handler.postDelayed(this, (current_location == null) ? 1000 : 10000);
            }
        };

    @Override
    public void onClick(View v){


        String color = (String) v.getTag();
        color = (color.equals("any")) ? null : color;
        Intent intent = new Intent(this, Map.class);
        if (color != null){
            intent.putExtra(Map.COLORMESSAGE, color);
        }

//        intent.putExtra(Map.SAVED_CURRENT_LOCATION, current_location);
//        Log.d("TRANSFER TO MAP", current_location + "");

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
        handler.removeCallbacks(watchLocation);
        paused = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(watchLocation);
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
