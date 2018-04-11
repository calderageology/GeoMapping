package com.caldera.geomapping.geomapping.ui.activities;


import android.Manifest;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.support.design.widget.Snackbar;
import android.widget.TextView;

import com.caldera.geomapping.geomapping.BuildConfig;
import com.caldera.geomapping.geomapping.R;
import com.caldera.geomapping.geomapping.models.objects.Station;

import com.caldera.geomapping.geomapping.services.LocationRequestHelper;
import com.caldera.geomapping.geomapping.services.LocationResultHelper;
import com.caldera.geomapping.geomapping.services.LocationUpdatesBroadcastReceiver;
import com.caldera.geomapping.geomapping.tasks.ReadFromDatabase;
import com.caldera.geomapping.geomapping.tasks.WriteToDatabase;
import com.caldera.geomapping.geomapping.ui.fragments.FragLocationDialog;
import com.caldera.geomapping.geomapping.ui.fragments.FragStationDetails;
import com.caldera.geomapping.geomapping.ui.fragments.FragStationList;
import com.caldera.geomapping.geomapping.ui.fragments.FragStationUpdateOrCreate;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

/**
 * Created by Michael on 22/03/2017.
 */

public class StationActivity extends AppCompatActivity implements
        FragStationList.FragmentItemClickCallback,
        FragStationDetails.FragmentEditItemCallback,
        FragStationUpdateOrCreate.FragmentStationUpdateOrCreateCallback,
        FragLocationDialog.FragmentLocationDialogCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SharedPreferences.OnSharedPreferenceChangeListener{

    private static final String TAG = StationActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    //Location constants
    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    // FIXME: 5/16/17
    private static final long UPDATE_INTERVAL = 10 * 1000;

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value, but they may be less frequent.
     */
    // FIXME: 5/14/17
    private static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;

    /**
     * The max time before batched results are delivered by location services. Results may be
     * delivered sooner than this interval.
     */
    private static final long MAX_WAIT_TIME = UPDATE_INTERVAL * 3;

    //UI Fragments
    private static final String FRAG_STATION_UPDATE_OR_CREATE = "FRAG_STATION_CREATE_OR_UPDATE";
    private static final String FRAG_STATION_LIST = "FRAG_STATION_LIST";
    private static final String FRAG_STATION_DETAILS = "FRAG_STATION_DETAILS";
    private static final String FRAG_LOCATION_DIALOG = "FRAG_LOCATION_DIALOG";

    private ArrayList<Station> listData;
    private Station lastStation;
    private FragmentManager manager;
    private TextView locationText;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * The entry point to Google Play Services.
     */
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station);

        locationText = (TextView) findViewById(R.id.lbl_activity_location);

        // Check if the user revoked runtime permissions.
        if (!checkPermissions()) {
            requestPermissions();
        }
        buildGoogleApiClient();

        manager = getSupportFragmentManager();
        loadStationList();
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        if(mGoogleApiClient.isConnected()) {
            Log.i("onResume", "GoogleApiClient is connected");
            requestLocationUpdates();
        }
        try {
            locationText.setText(LocationResultHelper.getSavedLocationResult(this));
            Log.i("onResume", "location result not null");
        } catch (NullPointerException e) {
            locationText.setText("Establishing position...");
            Log.e("onResume", "the saved results are empty");
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        Log.i(TAG, "Activity is destroyed");
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);

        if(mGoogleApiClient.isConnected()) {
            removeLocationUpdates();
        }
    }

    private void buildGoogleApiClient() {
        if (isGoogleApiClientConnected()) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    public boolean isGoogleApiClientConnected (){
        if(mGoogleApiClient != null){
            return true;
        }
        return false;
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */

    private void loadStationList() {
        ReadFromDatabase reader = new ReadFromDatabase(getApplicationContext());
        reader.setQueryCompleteListener(new ReadFromDatabase.OnQueryComplete(){
            @Override
            public void setQueryComplete(ArrayList result){
                listData = result;
                loadListFragment();
            }
        });
        reader.execute();
    }

    private void loadListFragment() {
        Fragment listFrag = FragStationList.newInstance(listData);
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);

        transaction.replace(R.id.cont_station_fragments, listFrag, FRAG_STATION_LIST);
        transaction.commit();
    }

    /**
     * Callback for FragStationList. Fires when the user clicks on an Item in the RecyclerView,
     * indicating that the user wishes to view details of the station object
     * @param position - The position in listData of the clicked item.
     */

    @Override
    public void onListItemClicked(int position) {
        Fragment detailFrag = FragStationDetails.newInstance(listData.get(position));
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);

        transaction.replace(R.id.cont_station_fragments, detailFrag, FRAG_STATION_DETAILS);
        transaction.commit();
    }

    @Override
    public void onAddStationButtonClicked() {


        /**
         * This is where the FragLocationDialog should appear overtop of the FragStationList and
         * show the location data and allow the user to accept the accuracy. Could also be used when
         * a map fragment in implemented in later versions
         */

        DialogFragment createFrag = FragLocationDialog.newInstance();
        createFrag.show(getFragmentManager(), FRAG_LOCATION_DIALOG);

    }


    /**
     * Callback to open FragStationUpdateOrCreate.java in order to modify an existing station object.
     * Calls newInstance() method which supplies an existing station object. See method
     * in FragStationUpdateOrCreate for details
     */

    @Override
    public void onEditButtonClick(Station station) {
        Fragment createFrag = FragStationUpdateOrCreate.newInstance(station);
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);

        transaction.replace(R.id.cont_station_fragments, createFrag, FRAG_STATION_UPDATE_OR_CREATE);
        transaction.commit();
    }

    /**
     * Callback to write incoming station object to the database. Once that operation is complete,
     * load FragStationList.java into the foreground.
     * @param station Either a newly created station, or an existing to do which will be updated.
     */

    @Override
    public void onDoneButtonClick(Station station) {
        WriteToDatabase writer = new WriteToDatabase(getApplicationContext(), station);
        writer.setWriteCompleteListener(new WriteToDatabase.OnWriteComplete() {
            @Override
            public void setWriteComplete(long result) {
                loadStationList();
            }
        });
        writer.execute();
    }

    @Override
    public void onBackPressed(){
        Fragment listFrag = manager.findFragmentByTag(FRAG_STATION_LIST);
        if (listFrag == null){
            listFrag = FragStationList.newInstance(listData);
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);

            transaction.replace(R.id.cont_station_fragments, listFrag, FRAG_STATION_LIST);
            transaction.commit();
        }
    }

    /**
     * Callback to open FragStationUpdateOrCreate.java in order to create a new station object.
     * actually edits an existing station that is created by the dialog fragment that contains
     * only location data.
     */

    @Override
    public void onAcceptAccuracyClicked(Station station) {
                        onEditButtonClick(station);
    }

    /**
     *
     * @param bundle Connecting to GoogleAPIClient
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, LocationUpdatesBroadcastReceiver.class);
        intent.setAction(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onConnectionSuspended(int i) {
        final String text = "Connection suspended";
        Log.w(TAG, text + ": Error code: " + i);
        showSnackbar("Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        final String text = "Exception while connecting to Google Play services";
        Log.w(TAG, text + ": " + connectionResult.getErrorMessage());
        showSnackbar(text);
    }
    /**
     * Shows a {@link Snackbar} using {@code text}.
     *
     * @param text The Snackbar text.
     */
    private void showSnackbar(final String text) {
        View container = findViewById(R.id.activity_station);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    findViewById(R.id.activity_station),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(StationActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(StationActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted. Kick off the process of building and connecting
                // GoogleApiClient.
                buildGoogleApiClient();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                Snackbar.make(
                        findViewById(R.id.activity_station),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        mLocationRequest.setInterval(UPDATE_INTERVAL);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        mLocationRequest.setMaxWaitTime(MAX_WAIT_TIME);
    }



    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(LocationResultHelper.KEY_LOCATION_UPDATES_RESULT)) {
            Log.i("PreferencesChanged", "broadcast sent");
            locationText.setText(LocationResultHelper.getSavedLocationResult(this));
        } else if (s.equals(LocationRequestHelper.KEY_LOCATION_UPDATES_REQUESTED)) {
            locationText.setText("Establishing position...");
            Log.i("PreferencesChanged", "broadcast not sent");
        }
    }

    /**
     * Start updating locations
     */
    public void requestLocationUpdates() {
        try {
            Log.i(TAG, "Starting location updates");
            LocationRequestHelper.setRequesting(this, true);
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, getPendingIntent());
        } catch (SecurityException e) {
            LocationRequestHelper.setRequesting(this, false);
            e.printStackTrace();
        }
    }

    /**
     * Stop updating locations
     */
    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        LocationRequestHelper.setRequesting(this, false);
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,
        getPendingIntent());
    }
}



