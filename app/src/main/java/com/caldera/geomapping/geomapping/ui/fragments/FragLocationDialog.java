package com.caldera.geomapping.geomapping.ui.fragments;

import android.app.DialogFragment;
import android.content.Context;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import com.caldera.geomapping.geomapping.R;
import com.caldera.geomapping.geomapping.models.objects.Station;
import com.caldera.geomapping.geomapping.services.LocationRequestHelper;
import com.caldera.geomapping.geomapping.services.LocationResultHelper;
import com.caldera.geomapping.geomapping.tasks.ReadFromDatabase;
import com.caldera.geomapping.geomapping.ui.activities.StationActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;


/**
 * Created by Michael on 15/11/2017.
 */
//TODO Have the app start the location service from the main activity and update the dialog textviews with Lat/Long/Acc when the dialog is created.
public class FragLocationDialog extends DialogFragment implements
        GoogleApiClient.OnConnectionFailedListener,
        SharedPreferences.OnSharedPreferenceChangeListener{

    private FragmentLocationDialogCallback callback;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    public String TAG = getClass().getSimpleName();

    Context activityContext;

    //UI Elements
    TextView stationNumber, accuracyText, locationText;
    Button proceed, cancel;


    ArrayList<Station> listData;


    Station lastStation, newStation;
    String stationNumberString, accuracy, easting, northing, location, precisionString;
    int oldStationNumber;

    public FragLocationDialog() {
    }

    public static FragLocationDialog newInstance(){
        FragLocationDialog fragment = new FragLocationDialog();
        return fragment;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public interface FragmentLocationDialogCallback {
        void onAcceptAccuracyClicked(Station station);
        void requestLocationUpdates();
        boolean isGoogleApiClientConnected();
        void removeLocationUpdates();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadStationNumber();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);
        View v = inflater.inflate(R.layout.fragment_location_dialog, container, false);

        stationNumber = (TextView) v.findViewById(R.id.lbl_dialog_station_number);
        locationText = (TextView) v.findViewById(R.id.lbl_dialog_location);
        accuracyText = (TextView) v.findViewById(R.id.lbl_dialog_accuracy);
        proceed = (Button) v.findViewById(R.id.btn_dialog_proceed);
        cancel = (Button) v.findViewById(R.id.btn_dialog_cancel);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        stationNumber.setText(stationNumberString);


        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        proceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    newStation = new Station("","","");
                    newStation.setStationNumber(stationNumberString);
                    newStation.setStationEasting(easting);
                    newStation.setStationNorthing(northing);
                    callback.onAcceptAccuracyClicked(newStation);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                dismiss();
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activityContext = context;
        if (context instanceof FragmentLocationDialogCallback){
            callback = (FragmentLocationDialogCallback) context;
        }else{
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener((SharedPreferences.OnSharedPreferenceChangeListener) getActivity());

        if(callback.isGoogleApiClientConnected()) {
            Log.i(TAG, "GoogleApiClient is connected");
            callback.requestLocationUpdates();
        }
        try {
            locationText.setText(LocationResultHelper.getSavedLocationResult(getActivity()));
            Log.i(TAG, "location result not null");
        } catch (NullPointerException e) {
            Log.e(TAG, "the saved results are empty");
            e.printStackTrace();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(LocationResultHelper.KEY_LOCATION_UPDATES_RESULT)) {
            Log.i("PreferencesChanged", "broadcast sent");
            locationText.setText(LocationResultHelper.getSavedLocationResult(getActivity()));
        } else if (s.equals(LocationRequestHelper.KEY_LOCATION_UPDATES_REQUESTED)) {
            Log.i("PreferencesChanged", "broadcast not sent");
        }
    }

    public void loadStationNumber(){
        ReadFromDatabase reader = new ReadFromDatabase(getActivity().getApplicationContext());
        reader.setQueryCompleteListener(new ReadFromDatabase.OnQueryComplete(){
            @Override
            public void setQueryComplete(ArrayList result){
                listData = result;

                if(!listData.isEmpty()){
                    lastStation = listData.get(listData.size()-1);
                    oldStationNumber = Integer.parseInt(lastStation.getStationNumber()) + 1;

                }else{
                    oldStationNumber = 0;

                }
                stationNumberString = Integer.toString(oldStationNumber);
                stationNumber.setText(stationNumberString);
            }
        });
        reader.execute();
    }

    /**
     * take a locations @para and add the easting, northing, and elevation
     * to an arraylist and calculate the average location from the list. Then
     * calculate the distance (in m) the @para location is to the average and
     * report as precision. Convert precision to string and return.
     */
    //TODO getLocationPrecision method needs to take easting northing instead of lat/long
/**    public String getLocationPrecision (Location currentLocation){
        //calculate the average
        if(precisionList == null){
            ArrayList<Location> precisionList = new ArrayList<>();
            precisionList.add(currentLocation);
            return "na";
        }
        precisionList.add(currentLocation);

        double averageLatitude = 0.0;
        double averageLongitude = 0.0;
        double averageElevation = 0.0;
        for(int i = 0; i <= precisionList.size(); i++){
            averageLatitude += precisionList.get(i).getLatitude();
            averageLongitude += precisionList.get(i).getLongitude();
            averageElevation += precisionList.get(i).getAltitude();

            }
        averageLocation.setLatitude(averageLatitude);
        averageLocation.setLongitude(averageLongitude);
        averageLocation.setAltitude(averageElevation);

        double currentEasting = new Deg2UTM(currentLocation).getEasting();
        double currentNorthing = new Deg2UTM(currentLocation).getNorthing();
        double currentElevation = currentLocation.setAltitude();

        double averageEasting = new Deg2UTM(averageLocation).getEasting();
        double averageNorthing = new Deg2UTM(averageLocation).getNorthing();


        return precisionString;
    }
 */
    /**
     * Ensures that only one button is enabled at any time. The Start Updates button is enabled
     * if the user is not requesting location updates. The Stop Updates button is enabled if the
     * user is requesting location updates.
     */
    /**
     *  private void updateButtonsState(boolean requestingLocationUpdates) {
        if (requestingLocationUpdates) {
        mRequestUpdatesButton.setEnabled(false);
        mRemoveUpdatesButton.setEnabled(true);
        } else {
        mRequestUpdatesButton.setEnabled(true);
        mRemoveUpdatesButton.setEnabled(false);
        }
        }

     */


}
