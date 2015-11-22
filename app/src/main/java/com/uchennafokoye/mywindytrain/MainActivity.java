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




//    private void initializeProgressDialog() {
//        progressDialog = new ProgressDialog(this);
//        progressDialog.setCancelable(true);
//        progressDialog.setMessage("Loading Closest Location");
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        progressDialog.setProgress(0);
//        progressDialog.setMax(100);
//        progressDialog.show();
//
//        progressBarStatus = 0;
//
//        new Thread(new Runnable() {
//            private int progressValueAnimatable = 0;
//
//            public void run() {
//
//                while (progressBarStatus < 100) {
//
//                    int currentProgressValue = getProgressValue();
//                    if (progressValueAnimatable >= currentProgressValue){
//                        if ((progressValueAnimatable + 10) < nextProgressLevel(currentProgressValue)) {
//                            progressValueAnimatable += 10;
//
//                        }
//                    } else {
//                        progressValueAnimatable = getProgressValue();
//                    }
//
//                    progressBarStatus = progressValueAnimatable;
//                    Log.d("ProgressValue", progressBarStatus + "");
//
//                    try {
//                        Thread.sleep(1000);
//
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                    progressBarHandler.post(new Runnable() {
//                        public void run() {
//                            progressDialog.setProgress(progressBarStatus);
//                        }
//                    });
//
//
//                }
//
//                if (progressBarStatus >= 100){
//                    try {
//                        Thread.sleep(1000);
//
//                    } catch(InterruptedException e){
//                        e.printStackTrace();
//                    }
//
//                    progressDialog.dismiss();
//
//                }
//
//            }
//
//            public int getProgressValue() {
//
//                if (current_location == null){
//                    return 50;
//                }
//
//                return 100;
//
//            }
//
//            public int nextProgressLevel(int progressValue) {
//
//                int nextProgressLevel;
//                switch (progressValue){
//                    case 0:
//                        nextProgressLevel = 10;
//                        break;
//                    case 10:
//                        nextProgressLevel = 50;
//                        break;
//                    case 50:
//                        nextProgressLevel = 70;
//                        break;
//                    case 70:
//                        nextProgressLevel = 99;
//                        break;
//                    case 99:
//                        nextProgressLevel = 100;
//                        break;
//                    default:
//                        nextProgressLevel = 100;
//                        break;
//                }
//
//                return nextProgressLevel;
//            }
//
//        }).start();
//    }






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, LocationService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
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

        Intent intent = getIntent();
        current_location = (LocationService.customLocation) intent.getSerializableExtra(Map.SAVED_CURRENT_LOCATION);
        Log.d("FROM_INTENT_MAIN", current_location + "");

        watchLocation();
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

        intent.putExtra(Map.SAVED_CURRENT_LOCATION, current_location);
        Log.d("TRANSFER TO MAP", current_location + "");

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
