package com.caldera.geomapping.geomapping.ui.activities;


import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.caldera.geomapping.geomapping.R;
import com.caldera.geomapping.geomapping.models.objects.Station;
import com.caldera.geomapping.geomapping.services.LocationAssistant;
import com.caldera.geomapping.geomapping.tasks.Deg2UTM;
import com.caldera.geomapping.geomapping.tasks.ReadFromDatabase;
import com.caldera.geomapping.geomapping.tasks.WriteToDatabase;
import com.caldera.geomapping.geomapping.ui.fragments.FragLocationDialog;
import com.caldera.geomapping.geomapping.ui.fragments.FragStationDetails;
import com.caldera.geomapping.geomapping.ui.fragments.FragStationList;
import com.caldera.geomapping.geomapping.ui.fragments.FragStationUpdateOrCreate;

import java.util.ArrayList;

/**
 * Created by Michael on 22/03/2017.
 */

public class StationActivity extends AppCompatActivity implements
        FragStationList.FragmentItemClickCallback,
        FragStationDetails.FragmentEditItemCallback,
        FragStationUpdateOrCreate.FragmentStationUpdateOrCreateCallback,
        FragLocationDialog.FragmentLocationDialogCallback,
        LocationAssistant.Listener {

    private static final String TAG = StationActivity.class.getSimpleName();

    //UI Fragments
    private static final String FRAG_STATION_UPDATE_OR_CREATE = "FRAG_STATION_CREATE_OR_UPDATE";
    private static final String FRAG_STATION_LIST = "FRAG_STATION_LIST";
    private static final String FRAG_STATION_DETAILS = "FRAG_STATION_DETAILS";
    private static final String FRAG_LOCATION_DIALOG = "FRAG_LOCATION_DIALOG";

    private ArrayList<Station> listData;
    private Station lastStation;
    private FragmentManager manager;
    private TextView locationText;
    private LocationAssistant assistant;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        assistant = new LocationAssistant(this, this, LocationAssistant.Accuracy.HIGH, 1000, false);
        assistant.setVerbose(true);

        setContentView(R.layout.activity_station);
        locationText = (TextView) findViewById(R.id.lbl_activity_location);
        locationText.setText(getString(R.string.empty));


        manager = getSupportFragmentManager();
        loadStationList();
    }



    @Override
    public void onResume() {
        super.onResume();
        assistant.start();

    }
    @Override
    public void onPause(){
        super.onPause();
        assistant.stop();
    }

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
     * Check and handle location permission requests and results
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (assistant.onPermissionsUpdated(requestCode, grantResults))
            locationText.setOnClickListener(null);
    }

    /**
     * The following methods are interfacing with the LocationAssistant
     */
    @Override
    public void onNeedLocationPermission() {
        locationText.setText("Need\nPermission");
        locationText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assistant.requestLocationPermission();
            }
        });
        assistant.requestAndPossiblyExplainLocationPermission();
    }

    @Override
    public void onExplainLocationPermission() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permissionExplanation)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        assistant.requestLocationPermission();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        locationText.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                assistant.requestLocationPermission();
                            }
                        });
                    }
                })
                .show();
    }

    @Override
    public void onLocationPermissionPermanentlyDeclined(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permissionPermanentlyDeclined)
                .setPositiveButton(R.string.ok, fromDialog)
                .show();
    }

    @Override
    public void onNeedLocationSettingsChange() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.switchOnLocationShort)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        assistant.changeLocationSettings();
                    }
                })
                .show();
    }

    @Override
    public void onFallBackToSystemSettings(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.switchOnLocationLong)
                .setPositiveButton(R.string.ok, fromDialog)
                .show();
    }

    /**
     * handle location requests
     * @param location
     */
    @Override
    public void onNewLocationAvailable(Location location) {
        if (location == null) return;

        double easting = new Deg2UTM(location).getEasting();
        double northing = new Deg2UTM(location).getNorthing();
        locationText.setOnClickListener(null);
        locationText.setText(easting + "\n" + northing);
        locationText.setAlpha(1.0f);
        locationText.animate().alpha(0.5f).setDuration(400);
    }

    @Override
    public void onMockLocationsDetected(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {
        locationText.setText(getString(R.string.mockLocationMessage));
        locationText.setOnClickListener(fromView);
    }

    @Override
    public void onError(LocationAssistant.ErrorType type, String message) {
        locationText.setText(getString(R.string.error));
    }
}



