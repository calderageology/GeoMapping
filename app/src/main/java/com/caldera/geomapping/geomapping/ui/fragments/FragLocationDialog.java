package com.caldera.geomapping.geomapping.ui.fragments;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import com.caldera.geomapping.geomapping.R;
import com.caldera.geomapping.geomapping.models.objects.Station;
import com.caldera.geomapping.geomapping.tasks.ReadFromDatabase;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;


/**
 * Created by Michael on 15/11/2017.
 */

public class FragLocationDialog extends DialogFragment {

    private FragmentLocationDialogCallback callback;

    private BroadcastReceiver broadcastReceiver;

    Context activityContext;

    TextView stationNumber, accuracyText;
    Button proceed, cancel;

    ArrayList<Station> listData;

    Station lastStation, newStation;
    String stationNumberString, accuracy, easting, northing, elevation, longitude, latitude;
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
        loadStationNumber();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);
        View v = inflater.inflate(R.layout.fragment_location_dialog, container, false);

        stationNumber = (TextView) v.findViewById(R.id.lbl_dialog_station_number);
        accuracyText = (TextView) v.findViewById(R.id.lbl_dialog_accuracy);
        proceed = (Button) v.findViewById(R.id.btn_dialog_proceed);
        cancel = (Button) v.findViewById(R.id.btn_dialog_cancel);

        return v;
    }


    @Override
    public void onResume(){
        super.onResume();

        if(broadcastReceiver == null) {
            Log.d("onStart: ", "no broadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {

                    easting = String.valueOf(intent.getExtras().get("easting"));
                    northing = String.valueOf(intent.getExtras().get("northing"));
                    elevation = String.valueOf(intent.getExtras().get("elevation"));
                    longitude = String.valueOf(intent.getExtras().get("longitude"));
                    latitude = String.valueOf(intent.getExtras().get("latitude"));

                    accuracy = String.valueOf(intent.getExtras().get("accuracy"));
                    accuracyText.setText("Accuracy: " + accuracy + "m");

                    Log.d("onReceive: ", "Accuracy: " + accuracy);
                }
            };
        }

        Log.d("onResume: ", "broadcastReceiver created");
        Log.d("after onReceive: ", "Accuracy: " + accuracy);


        activityContext.registerReceiver(broadcastReceiver, new IntentFilter("location_update"));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (broadcastReceiver != null) {
            activityContext.unregisterReceiver(broadcastReceiver);
            Log.d("onPause: ", "broadcastReceiver unregistered");
        }
    }



        //TODO the broadcast is either not received or not sending?




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
}
