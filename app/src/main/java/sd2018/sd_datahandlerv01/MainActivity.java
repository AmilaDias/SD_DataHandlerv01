package sd2018.sd_datahandlerv01;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.battery.BatteryState;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.afinal.core.AsyncTask;
import dji.common.flightcontroller.*;


public class MainActivity extends AppCompatActivity {

    /**
     * Global variables
     */
    //Public variables
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";

    //Private variables
    private static final String TAG = MainActivity.class.getName();
    private static BaseProduct mProduct;
    private Handler mHandler;
    private FlightControllerState flightControllerInfo;
    private LocationCoordinate3D droneCoordinates3D;
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static final int REQUEST_PERMISSION_CODE = 12345;
    private DatabaseReference databaseRef;
    private DroneTelemetryData droneData = new DroneTelemetryData();
    private int dummyBattery;
    BatteryState droneBattery;


    /**
     * Permissions required to run application
     */
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            android.Manifest.permission.VIBRATE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.WAKE_LOCK,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.CHANGE_WIFI_STATE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_PHONE_STATE,
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        * DJI Project verification
        * */
//        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            checkAndRequestPermissions();
//        }
        setContentView(R.layout.activity_main);

        //////////////////////TEST////////////////////
        final Button dummyButton = findViewById(R.id.mStartButton);
        dummyBattery = 100;
        dummyButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                for (int i = 0; i < 101; i++)
                {
                    setDUMMYDATA();
                    syncToFirebase();
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    dummyBattery--;
                }
            }
        });
        //////////////////////////////////////////////

        //Initialize DJI SDK Manager
//        mHandler = new Handler(Looper.getMainLooper());

        //Initialize Google Firebase Database
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseRef = database.getReference();

        //Initialize drone telemetry producers
        flightControllerInfo = new FlightControllerState();
        droneCoordinates3D = new LocationCoordinate3D(0,0,0);
    }


    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showToast("Need to grant the permissions!");
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

/**
 ///////////////////////////////////////////DJI CODE///////////////////////////////////////////////////
 */


    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            showToast("Missing permissions!!!");
        }
    }


    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    showToast("registering, pls wait...");
                    DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                showToast("Register Success");
                                DJISDKManager.getInstance().startConnectionToProduct();
                            } else {
                                showToast("Register sdk fails, please check the bundle id and network connection!");
                            }
                            Log.v(TAG, djiError.getDescription());
                        }
                        @Override
                        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
                            mProduct = newProduct;
                            if(mProduct != null) {
                                mProduct.setBaseProductListener(mDJIBaseProductListener);
                            }
                            notifyStatusChange();
                        }
                    });
                }
            });
        }
    }


    private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {
        @Override
        public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {
            if(newComponent != null) {
                newComponent.setComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };


    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };


    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }


    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };


    private void showToast(final String toastMsg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
            }
        });
    }


/**
 ///////////////////////////////////////////SD2018 Code///////////////////////////////////////////////////
 */

    private void getTelemetryData(){
        droneData.setAltitude(droneCoordinates3D.getAltitude());
        droneData.setCurrLatitude(droneCoordinates3D.getLatitude());
        droneData.setCurrLongitude(droneCoordinates3D.getLongitude());
        droneData.setAirSpeed(flightControllerInfo.getVelocityX(),flightControllerInfo.getVelocityY());
        droneData.setBatteryPercentage(droneBattery.getChargeRemainingInPercent());
    }

    private void syncToFirebase(){
        //Latitude
        databaseRef.child("Drone Telemetry").child("liveData").child("1").setValue(droneData.getCurrLatitude());
        //Longitude
        databaseRef.child("Drone Telemetry").child("liveData").child("2").setValue(droneData.getCurrLongitude());
        //Altitude
        databaseRef.child("Drone Telemetry").child("liveData").child("3").setValue(droneData.getAltitude());
        //Battery
        databaseRef.child("Drone Telemetry").child("liveData").child("4").setValue(droneData.getBatteryPercentage());
        //Speed
        databaseRef.child("Drone Telemetry").child("liveData").child("5").setValue(droneData.getAirSpeed());
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setDUMMYDATA(){
        droneData.setAltitude(setAlitude());
        droneData.setCurrLatitude(setLatitude());
        droneData.setCurrLongitude(setLongitude());
        droneData.setAirSpeed(setAirSpeed(), setAirSpeed());
        droneData.setBatteryPercentage(dummyBattery);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private double setLatitude(){
        double tempLat = ThreadLocalRandom.current().nextDouble(-90, 90 + 1);
        DecimalFormat df = new DecimalFormat("##.###");
        return Double.parseDouble(df.format(tempLat));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private double setLongitude(){
        double tempLong = ThreadLocalRandom.current().nextDouble(-90, 90 + 1);
        DecimalFormat df = new DecimalFormat("##.###");
        return Double.parseDouble(df.format(tempLong));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private double setAlitude(){
        double tempAlt = ThreadLocalRandom.current().nextDouble(15);
        DecimalFormat df = new DecimalFormat("##.#");
        return Double.parseDouble(df.format(tempAlt));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private double setAirSpeed(){
        double tempSpeed = ThreadLocalRandom.current().nextDouble(30);
        DecimalFormat df = new DecimalFormat("##.##");
        return Double.parseDouble(df.format(tempSpeed));
    }



}

