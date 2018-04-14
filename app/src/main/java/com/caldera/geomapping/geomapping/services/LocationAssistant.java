package com.caldera.geomapping.geomapping.services;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;


public class LocationAssistant implements  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public interface Listener {
        void onNeedLocationPermission();
        void onExplainLocationPermission();
        void onLocationPermissionPermanentlyDeclined
                (View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog);
        void onNeedLocationSettingsChange();
        void onFallBackToSystemSettings
                (View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog);
        void onNewLocationAvailable(Location location);
        void onMockLocationsDetected
                (View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog);
        void onError(ErrorType type, String message);
    }

    public enum Accuracy {
        HIGH, MEDIUM, LOW, PASSIVE
    }

    public enum ErrorType{
        SETTINGS, RETRIEVAL
    }

    private final int REQUEST_CHECK_SETTINGS = 0;
    private final int REQUEST_LOCATION_PERMISSION = 1;

    // Parameters
    protected Context context;
    private Activity activity;
    private Listener listener;
    private int priority;
    private long updateInterval;
    private boolean allowMockLocations;
    private boolean verbose;
    private boolean quiet;

    //Internal state
    private boolean permissionGranted;
    private boolean locationRequested;
    private boolean locationStatusOk;
    private boolean changeSettings;
    private boolean updatesRequested;
    protected Location bestLocation;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Status locationStatus;
    private boolean mockLocationsEnabled;
    private int numTimesPermissionDeclined;

    //Mock location rejection
    private Location lastMockLocation;
    private int numGoodReadings;

    public LocationAssistant(final Context context, Listener listener, Accuracy accuracy,
                             long updateInterval, boolean allowMockLocations){
        this.context = context;
        if(context instanceof Activity)
            this.activity = (Activity) context;
        this.listener = listener;
        switch(accuracy){
            case HIGH:
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
                break;
            case MEDIUM:
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
                break;
            case LOW:
                priority = LocationRequest.PRIORITY_LOW_POWER;
                break;
            case PASSIVE:
                default:
                priority = LocationRequest.PRIORITY_NO_POWER;
        }
        this.updateInterval = updateInterval;
        this.allowMockLocations = allowMockLocations;

        if(googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    public void setVerbose(boolean verbose){
        this.verbose = verbose;
    }

    public void setQuiet(boolean quiet){
        this.quiet = quiet;
    }

    public void start(){
        checkMockLocations();
        googleApiClient.connect();
    }

    public void register(Activity activity, Listener listener){
        this.activity = activity;
        this.listener = listener;
        checkInitialLocation();
        acquireLocation();
    }

    public void stop(){
        if(googleApiClient.isConnected()){
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
        permissionGranted = false;
        locationRequested = false;
        locationStatusOk = false;
        updatesRequested = false;
    }

    public void unregister(){
        this.activity = null;
        this.listener = null;
    }

    public void reset(){
        permissionGranted = false;
        locationRequested = false;
        locationStatusOk = false;
        updatesRequested = false;
        acquireLocation();
    }

    public Location getBestLocation(){
        return bestLocation;
    }


    public void requestAndPossiblyExplainLocationPermission() {
        if (permissionGranted) return;
        if (activity == null) {
            if (!quiet)
                Log.e(getClass().getSimpleName(), "Need location permission, but no activity is registered! " +
                        "Specify a valid activity when constructing " + getClass().getSimpleName() +
                        " or register it explicitly with register().");
            return;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                && listener != null)
            listener.onExplainLocationPermission();
        else
            requestLocationPermission();
    }


    public void requestLocationPermission() {
        if (activity == null) {
            if (!quiet)
                Log.e(getClass().getSimpleName(), "Need location permission, but no activity is registered! " +
                        "Specify a valid activity when constructing " + getClass().getSimpleName() +
                        " or register it explicitly with register().");
            return;
        }
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
    }


    public boolean onPermissionsUpdated(int requestCode, int[] grantResults) {
        if (requestCode != REQUEST_LOCATION_PERMISSION) return false;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            acquireLocation();
            return true;
        } else {
            numTimesPermissionDeclined++;
            if (!quiet)
                Log.i(getClass().getSimpleName(), "Location permission request denied.");
            if (numTimesPermissionDeclined >= 2 && listener != null)
                listener.onLocationPermissionPermanentlyDeclined(onGoToAppSettingsFromView,
                        onGoToAppSettingsFromDialog);
            return false;
        }
    }

    public void onActivityResult(int requestCode, int resultCode){
        if(requestCode != REQUEST_CHECK_SETTINGS) return;
        if(resultCode == Activity.RESULT_OK){
            changeSettings = false;
            locationStatusOk = true;
        }
        acquireLocation();
    }

    public void changeLocationSettings(){
        if(locationStatus == null)return;
        if(activity == null){
            if(!quiet)
                Log.e(getClass().getSimpleName(), "Need to resolve status issue, but no activity is " +
                "registered! Specify a valid activity when constructing " + getClass().getSimpleName() +
                " or register it explicitly with register().");
            return;
        }
        try {
            locationStatus.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
        }catch (IntentSender.SendIntentException e){
            if(!quiet)
                if (!quiet)
                    Log.e(getClass().getSimpleName(), "Error while attempting to resolve location status issues:\n" +
                            e.toString());
            if (listener != null)
                listener.onError(ErrorType.SETTINGS, "Could not resolve location settings issue:\n" +
                        e.getMessage());
            changeSettings = false;
            acquireLocation();
        }
    }

    protected void acquireLocation(){
        if(!permissionGranted) checkLocationPermission();
        if(!permissionGranted){
            if(numTimesPermissionDeclined >= 2)return;
            if(listener != null)
                listener.onNeedLocationPermission();
            else if(!quiet)
                Log.e(getClass().getSimpleName(),"Need location permission, but no listener is registered! " +
                        "Specify a valid listener when constructing " + getClass().getSimpleName() +
                        " or register it explicitly with register().");
            return;
        }
        if(!locationRequested){
            requestLocation();
            return;
        }
        if(!locationStatusOk){
            if(changeSettings){
                if(listener != null)
                    listener.onNeedLocationSettingsChange();
                else if(!quiet)
                    Log.e(getClass().getSimpleName(), "Need location settings change, but no listener " +
                        "is registered! Specify a valid listener when constructing " + getClass().getSimpleName() +
                        " or register it explicitly with register().");

            }else
                checkProviders();
            return;
        }
        if(!updatesRequested){
            requestLocationUpdates();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    acquireLocation();
                }
            },10000);
            return;
        }
        if(!checkLocationAvailability()){
            checkProviders();
        }
    }

    protected void checkInitialLocation(){
        if(!googleApiClient.isConnected() || !permissionGranted || !locationRequested || !locationStatusOk)return;
        try{
            Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            onLocationChanged(location);
        }catch (SecurityException e){
            if (!quiet)
                Log.e(getClass().getSimpleName(), "Error while requesting last location:\n " +
                e.toString());
            if(listener != null)
                listener.onError(ErrorType.RETRIEVAL, "Could not retrieve initial location:\n " +
                e.getMessage());
        }
    }

    private void checkMockLocations(){
        if(Build.VERSION.SDK_INT < 18 &&
                !android.provider.Settings.Secure.getString(context.getContentResolver(), Settings
                .Secure.ALLOW_MOCK_LOCATION).equals("0")){
            mockLocationsEnabled = true;
            if(listener != null)
                listener.onMockLocationsDetected(onGoToDevSettingsFromView, onGoToDevSettingsFromDialog);
        }else
            mockLocationsEnabled = false;
    }

    private void checkLocationPermission(){
        permissionGranted = Build.VERSION.SDK_INT < 23 ||
                ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocation(){
        if(!googleApiClient.isConnected() || !permissionGranted) return;
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(priority);
        locationRequest.setInterval(updateInterval);
        locationRequest.setFastestInterval(updateInterval);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);
        LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
                .setResultCallback(onLocationSettingsReceived);
    }

    private boolean checkLocationAvailability(){
        if(!googleApiClient.isConnected() || !permissionGranted)return false;
        try{
            LocationAvailability la = LocationServices.FusedLocationApi.getLocationAvailability(googleApiClient);
            return (la != null && la.isLocationAvailable());
        }catch (SecurityException e){
            if(!quiet)
                Log.e(getClass().getSimpleName(), "Error while checking location availability:\n" +
                e.toString());
            if(listener != null)
                listener.onError(ErrorType.RETRIEVAL, "Could not check location availability:\n" +
                e.getMessage());
            return false;
        }
    }

    public void checkProviders(){
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) return;
        if (listener != null)
            listener.onFallBackToSystemSettings(onGoToLocationSettingsFromView, onGoToLocationSettingsFromDialog);
        else if (!quiet)
            Log.e(getClass().getSimpleName(), "Location providers need to be enabled, but no listener is " +
                    "registered! Specify a valid listener when constructing " + getClass().getSimpleName() +
                    " or register it explicitly with register().");
    }

    private void requestLocationUpdates(){
        if (!googleApiClient.isConnected() || !permissionGranted || !locationRequested) return;
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            updatesRequested = true;
        } catch (SecurityException e) {
            if (!quiet)
                Log.e(getClass().getSimpleName(), "Error while requesting location updates:\n " +
                        e.toString());
            if (listener != null)
                listener.onError(ErrorType.RETRIEVAL, "Could not request location updates:\n" +
                        e.getMessage());
        }
    }

    private DialogInterface.OnClickListener onGoToLocationSettingsFromDialog = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (activity != null) {
                Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivity(intent);
            } else if (!quiet)
                Log.e(getClass().getSimpleName(), "Need to launch an intent, but no activity is registered! " +
                        "Specify a valid activity when constructing " + getClass().getSimpleName() +
                        " or register it explicitly with register().");
        }
    };

    private View.OnClickListener onGoToLocationSettingsFromView = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (activity != null) {
                Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivity(intent);
            } else if (!quiet)
                Log.e(getClass().getSimpleName(), "Need to launch an intent, but no activity is registered! " +
                        "Specify a valid activity when constructing " + getClass().getSimpleName() +
                        " or register it explicitly with register().");
        }
    };

    private DialogInterface.OnClickListener onGoToDevSettingsFromDialog = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (activity != null) {
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                activity.startActivity(intent);
            } else if (!quiet)
                Log.e(getClass().getSimpleName(), "Need to launch an intent, but no activity is registered! " +
                        "Specify a valid activity when constructing " + getClass().getSimpleName() +
                        " or register it explicitly with register().");
        }
    };

    private View.OnClickListener onGoToDevSettingsFromView = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (activity != null) {
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                activity.startActivity(intent);
            } else if (!quiet)
                Log.e(getClass().getSimpleName(), "Need to launch an intent, but no activity is registered! " +
                        "Specify a valid activity when constructing " + getClass().getSimpleName() +
                        " or register it explicitly with register().");
        }
    };

    private DialogInterface.OnClickListener onGoToAppSettingsFromDialog = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (activity != null) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            } else if (!quiet)
                Log.e(getClass().getSimpleName(), "Need to launch an intent, but no activity is registered! " +
                        "Specify a valid activity when constructing " + getClass().getSimpleName() +
                        " or register it explicitly with register().");
        }
    };

    private View.OnClickListener onGoToAppSettingsFromView = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (activity != null) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            } else if (!quiet)
                Log.e(getClass().getSimpleName(), "Need to launch an intent, but no activity is registered! " +
                        "Specify a valid activity when constructing " + getClass().getSimpleName() +
                        " or register it explicitly with register().");
        }
    };

    private boolean isLocationPlausible(Location location) {
        if (location == null) return false;

        boolean isMock = mockLocationsEnabled || (Build.VERSION.SDK_INT >= 18 && location.isFromMockProvider());
        if (isMock) {
            lastMockLocation = location;
            numGoodReadings = 0;
        } else
            numGoodReadings = Math.min(numGoodReadings + 1, 1000000); // Prevent overflow

        // We only clear that incident record after a significant show of good behavior
        if (numGoodReadings >= 20) lastMockLocation = null;

        // If there's nothing to compare against, we have to trust it
        if (lastMockLocation == null) return true;

        // And finally, if it's more than 1km away from the last known mock, we'll trust it
        double d = location.distanceTo(lastMockLocation);
        return (d > 1000);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        acquireLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;
        boolean plausible = isLocationPlausible(location);
        if (verbose && !quiet)
            Log.i(getClass().getSimpleName(), location.toString() +
                    (plausible ? " -> plausible" : " -> not plausible"));

        if (!allowMockLocations && !plausible) {
            if (listener != null) listener.onMockLocationsDetected(onGoToDevSettingsFromView,
                    onGoToDevSettingsFromDialog);
            return;
        }

        bestLocation = location;
        if (listener != null)
            listener.onNewLocationAvailable(location);
        else if (!quiet)
            Log.w(getClass().getSimpleName(), "New location is available, but no listener is registered!\n" +
                    "Specify a valid listener when constructing " + getClass().getSimpleName() +
                    " or register it explicitly with register().");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (!quiet)
            Log.e(getClass().getSimpleName(), "Error while trying to connect to Google API:\n" +
                    connectionResult.getErrorMessage());
        if (listener != null)
            listener.onError(ErrorType.RETRIEVAL, "Could not connect to Google API:\n" +
                    connectionResult.getErrorMessage());
    }

    ResultCallback<LocationSettingsResult> onLocationSettingsReceived = new ResultCallback<LocationSettingsResult>() {
        @Override
        public void onResult(@NonNull LocationSettingsResult result) {
            locationRequested = true;
            locationStatus = result.getStatus();
            switch (locationStatus.getStatusCode()) {
                case LocationSettingsStatusCodes.SUCCESS:
                    locationStatusOk = true;
                    checkInitialLocation();
                    break;
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    locationStatusOk = false;
                    changeSettings = true;
                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    locationStatusOk = false;
                    break;
            }
            acquireLocation();
        }
    };

}
