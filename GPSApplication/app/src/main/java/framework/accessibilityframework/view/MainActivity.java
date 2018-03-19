package framework.accessibilityframework.view;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

import framework.accessibilityframework.R;
import framework.accessibilityframework.control.PermissionUtils;
import framework.accessibilityframework.view.sensor.positionsensor.gps.LocationActivity;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    //layout stuff
    EditText mTimeEditText;//seconds
    EditText mDistanceEditText;//meters
    Button mStartButton;

    private static final int REQ_LOCATION = 5;
    private static final String PERMISSION_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private boolean allPermissionsGranted = false;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private boolean mRequestingLocationUpdates = true;
    Location mStartLocation;

    //
    private float mCurrentDistance = 0; //meters
    private float mMaxDistance; //meters
    private static final int FASTEST_UPDATE_INTERVAL = 5000;
    private long mTimeBetweenUpdates = 30000; //time in MILLISECONDS between location updates. TODO Change this according to the app's needs


    private static final int MSG_SHOW_DISTANCE_TOAST = 100;

    private Handler mHandler = new Handler();
    private int mToastDelay = 5000;
    private Runnable mToastRunnable;
    private boolean mToastThreadRunning = false;

    private Handler mIncomingHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SHOW_DISTANCE_TOAST:
                    //show mCurrentDistance by toast and vibration
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (v != null) {
                        v.vibrate(50);
                    }
                    String toastText = getString(R.string.travelled_distance) + mCurrentDistance + "m";
                    Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_main);

        mTimeEditText = (EditText) findViewById(R.id.time_edit_text);
        mDistanceEditText = (EditText) findViewById(R.id.distance_edit_text);
        mStartButton = (Button) findViewById(R.id.start_button);

        //location provider client
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        //check for permissions and these stuff
        String[] record_perm = {PERMISSION_LOCATION};
        if (Build.VERSION.SDK_INT >= 16 && !allPermissionsGranted) { //has Android 6.0, but no permission was granted for camera and storage

            if (PermissionUtils.checkPermission(MainActivity.this, PERMISSION_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                PermissionUtils.requestPermission(MainActivity.this, record_perm, REQ_LOCATION);
            }

            if (PermissionUtils.checkPermission(MainActivity.this, PERMISSION_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = true;
            }
        }


        if (allPermissionsGranted) {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                mStartLocation = location;
                            }
                        }
                    });
        }

        //set up LocationCallBack to update the mCurrentDistance and show dialog at the end
        mLocationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                //get locations
                List<Location> locations = locationResult.getLocations();
                Log.d(TAG, locations.toString());

                //calculate the travelled distance
                Location lastLocation = locationResult.getLastLocation();
                mCurrentDistance = lastLocation.distanceTo(mStartLocation);

                if (mCurrentDistance >= mMaxDistance) {
                    popUpFinalDialog();
                    stopToastThread();
                }

            }

            ;

        };

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRequestingLocationUpdates) {
                    //get time from editText and convert to milliseconds
                    mToastDelay = Integer.parseInt(mTimeEditText.getText().toString()) * 1000;
                    //set the same time for mTimeBetweenUpdates
                    mTimeBetweenUpdates = mToastDelay;

                    //get maxDistance from editText
                    mMaxDistance = Float.parseFloat(mDistanceEditText.getText().toString());

                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            mIncomingHandler.sendEmptyMessage(MSG_SHOW_DISTANCE_TOAST);

                            mToastRunnable = this;
                            mToastThreadRunning = true;

                            mHandler.postDelayed(mToastRunnable, mToastDelay);
                        }
                    }, mToastDelay);

                    try {
                        //create request
                        mLocationRequest = createLocationRequest();


                        //pull location request
                        if (allPermissionsGranted) {
                            if (mRequestingLocationUpdates) {
                                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    // TODO: Consider calling
                                    //    ActivityCompat#requestPermissions
                                    // here to request the missing permissions, and then overriding
                                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                    //                                          int[] grantResults)
                                    // to handle the case where the user grants the permission. See the documentation
                                    // for ActivityCompat#requestPermissions for more details.
                                    return;
                                }
                                mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                        mLocationCallback,
                                        null /* Looper */);
                            }

                        }else{
                            Toast.makeText(MainActivity.this, "Permissions are missing", Toast.LENGTH_LONG).show();
                        }
                    } catch (ActivityNotFoundException a) {
                        Toast.makeText(MainActivity.this,
                                "Opps! Your device doesn’t support Location Services", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    void popUpFinalDialog(){
        //set up an alertDialog builder
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
        alertBuilder.setMessage(R.string.final_congratulation);
        alertBuilder.setNegativeButton(R.string.exit_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onExitCommand();
            }
        });
        alertBuilder.setPositiveButton(R.string.restart_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onRestartCommand();
            }
        });

        //create and show the alert dialog
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    public void onRestartCommand() {
        finish();
        startActivity(getIntent());
    }

    public void onExitCommand() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory( Intent.CATEGORY_HOME );
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
        finish();
    }


    @Override
    protected void onPause() {
        super.onPause();
        mRequestingLocationUpdates = false;
        stopLocationUpdates();
        stopToastThread();

    }

    private void stopToastThread(){
        if(mToastThreadRunning){
            mHandler.removeCallbacks(mToastRunnable); //stop handler when activity not visible
            mToastThreadRunning = false;
        }
    }

    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates)
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(mTimeBetweenUpdates);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent i = new Intent(MainActivity.this, LocationActivity.class);
            startActivity(i);
        } else{
            Toast.makeText(getApplicationContext(),"Opps! Your device doesn’t support Location Services" , Toast.LENGTH_LONG).show();
        }
    }

}
