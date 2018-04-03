package com.caldera.geomapping.geomapping.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;

import com.caldera.geomapping.geomapping.tasks.Deg2UTM;


/**
 * Created by Michael on 22/03/2017.
 *
 */

public class GPSService extends Service{

    private LocationManager manager;
    private LocationListener listener;
    private Deg2UTM deg2UTM;
    private double easting, northing;
    private String accuracy;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        //TODO onLocationChanged is not called, accuracy toString is null
        listener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                Log.i("onLocationChanged: ", "called");
                /**
                 * This is suppose to convert the degrees coordinates into UTM. I'm not sure this
                 * will work and the math running on the activity thread may slow the app or even
                 * time it out.
                 *
                 */
                Deg2UTM deg2UTM = new Deg2UTM(location);
                easting = deg2UTM.getEasting();
                northing = deg2UTM.getNorthing();

                //keyword pair the values and send the broadcast
                Intent i = new Intent("location_update");
                /**
                 * i.putExtra("longitude", location.getLongitude());
                 * i.putExtra("latitude", location.getLatitude());
                 * i.putExtra("elevation", location.getAltitude());
                 *
                 * i.putExtra("easting", easting);
                 * i.putExtra("northing", northing);
                 */

                i.putExtra("accuracy", location.getAccuracy());


                sendBroadcast(i);

            }


            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);

            }
        };


        Log.d("GPSService.onCreate: ", "listener created");

        manager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        //noinspection MissingPermission
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,1,listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(manager != null){
            manager.removeUpdates(listener);
        }
    }
}