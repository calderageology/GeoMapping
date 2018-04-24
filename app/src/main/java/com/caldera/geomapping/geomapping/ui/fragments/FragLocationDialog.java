package com.caldera.geomapping.geomapping.ui.fragments;

import android.app.DialogFragment;
import android.content.Context;

import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import com.caldera.geomapping.geomapping.R;
import com.caldera.geomapping.geomapping.models.objects.Station;
import com.caldera.geomapping.geomapping.services.LocationAssistant;
import com.caldera.geomapping.geomapping.tasks.Deg2UTM;
import com.caldera.geomapping.geomapping.tasks.GeoMath;
import com.caldera.geomapping.geomapping.tasks.ReadFromDatabase;

import java.util.ArrayList;


/**
 * Created by Michael on 15/11/2017.
 */

public class FragLocationDialog extends DialogFragment implements LocationAssistant.Listener{

    private FragmentLocationDialogCallback callback;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    public String TAG = getClass().getSimpleName();

    Context activityContext;

    //UI Elements
    TextView stationNumber, eastingText, northingText, precisionText;
    Button proceed, cancel;


    ArrayList<Station> listData;
    LocationAssistant assistant;
    ArrayList<Location> locations = new ArrayList<>();
    Location avgLocation;


    Station lastStation, newStation;
    double easting, northing, elevation;
    long time;
    String stationNumberString;
    int oldStationNumber;

    public FragLocationDialog() {
    }

    public static FragLocationDialog newInstance(){
        FragLocationDialog fragment = new FragLocationDialog();
        return fragment;
    }




    public interface FragmentLocationDialogCallback {
        void onAcceptAccuracyClicked(Station station);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        assistant = new LocationAssistant(getActivity(), this, LocationAssistant.Accuracy.HIGH, 1000, false);
        assistant.setVerbose(true);



        loadStationNumber();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);
        View v = inflater.inflate(R.layout.fragment_location_dialog, container, false);

        stationNumber = (TextView) v.findViewById(R.id.lbl_dialog_station_number);
        precisionText = (TextView) v.findViewById(R.id.lbl_dialog_precision);
        eastingText = (TextView) v.findViewById(R.id.lbl_dialog_easting);
        northingText = (TextView) v.findViewById(R.id.lbl_dialog_northing);


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
                    newStation = new Station("","","","","","");
                    newStation.setStationNumber(stationNumberString);
                    newStation.setStationEasting(String.valueOf(easting));
                    newStation.setStationNorthing(String.valueOf(northing));
                    newStation.setStationElevation(String.valueOf(elevation));
                    newStation.setStationTime(String.valueOf(time));
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
        assistant.start();
    }
    @Override
    public void onPause() {
        super.onPause();
        assistant.stop();
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

    @Override
    public void onNeedLocationPermission() {

    }

    @Override
    public void onExplainLocationPermission() {

    }

    @Override
    public void onLocationPermissionPermanentlyDeclined(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {

    }

    @Override
    public void onNeedLocationSettingsChange() {

    }

    @Override
    public void onFallBackToSystemSettings(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {

    }

    @Override
    public void onNewLocationAvailable(Location location) {
        if (location == null) return;

        GeoMath geoMath = new GeoMath();

        try{
            locations.add(location);
            if(locations.size() <= 4){
                precisionText.setText("Hold still, averaging your location from " + locations.size() + " readings.");

            }else {
                double precision = geoMath.getPrecision(locations, location);

                avgLocation = geoMath.getAverageLocation(locations);

                easting = new Deg2UTM(avgLocation).getEasting();
                northing = new Deg2UTM(avgLocation).getNorthing();
                elevation = avgLocation.getAltitude();
                time = avgLocation.getTime();

                eastingText.setOnClickListener(null);
                northingText.setOnClickListener(null);
                eastingText.setText(easting + "E");
                northingText.setText(northing + "N");
                precisionText.setText("Reading " + locations.size() + " is " + precision + "m from the average.");
                eastingText.setAlpha(1.0f);
                northingText.setAlpha(1.0f);
                precisionText.setAlpha(1.0f);
                eastingText.animate().alpha(0.5f).setDuration(400);
                northingText.animate().alpha(0.5f).setDuration(400);
                precisionText.animate().alpha(0.5f).setDuration(400);


            }
        }catch(NullPointerException e){
            Log.e(TAG,"Locations is null ", e);
        }

    }



    @Override
    public void onMockLocationsDetected(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {

    }

    @Override
    public void onError(LocationAssistant.ErrorType type, String message) {

    }

}
