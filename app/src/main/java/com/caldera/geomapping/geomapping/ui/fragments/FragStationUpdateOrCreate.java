package com.caldera.geomapping.geomapping.ui.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.caldera.geomapping.geomapping.R;
import com.caldera.geomapping.geomapping.models.objects.Station;

public class FragStationUpdateOrCreate extends Fragment {
    private static final String STATION = "STATION";

    private Station station;
    private EditText easting, northing;
    private TextView stationNumber, eastingTitle, northingTitle;
    private Button done;

    private BroadcastReceiver broadcastReceiver;
    private String location;

    private FragmentStationUpdateOrCreateCallback callback;

    public FragStationUpdateOrCreate() {
    }

    /**
     * In the event that we wish to Edit an existing station object, use this method to create
     * an instance of the fragment. Otherwise, see Fragment below.
     * @param station this must be a station object which already exists
     * @return Fragment Instance. Super useful explanation, amirite?
     */
    public static FragStationUpdateOrCreate newInstance(Station station) {
        FragStationUpdateOrCreate fragment = new FragStationUpdateOrCreate();
        Bundle args = new Bundle();
        args.putParcelable(STATION, station);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * When we wish to create a new station object to be added to the database, we'll use this method
     * to get an instance of the fragment. With the creation of the FragLocationDialog there may be
     * no need for a new instance of FragStationUpdateOrCreate that does not pass in a station object.
     * The FragLocationDialog can create a new station with only the location data and all other fields
     * blank. then pass that to the FragStationUpdateOrCreate to edit the other fields.
     * @return
     */
    public static FragStationUpdateOrCreate newInstance() {
        FragStationUpdateOrCreate fragment = new FragStationUpdateOrCreate();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.station = getArguments().getParcelable(STATION);
        } else {

            station = new Station("", "", "");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);
/**
 * replace views
 */
        View v = inflater.inflate(R.layout.fragment_station_update_or_create, container, false);
        stationNumber = (TextView) v.findViewById(R.id.lbl_station_number);
        eastingTitle = (TextView) v.findViewById(R.id.lbl_station_easting);
        easting = (EditText) v.findViewById(R.id.edt_station_easting);
        northingTitle = (TextView) v.findViewById(R.id.lbl_station_northing);
        northing = (EditText) v.findViewById(R.id.edt_station_northing);
        done = (Button) v.findViewById(R.id.btn_done);

        return v;
    }

    /**
     * If station object exists (i.e. was passed in as an argument), update text fields with data
     * from existing station. Otherwise, create a new station object.
     * @param savedInstanceState
     */
    public void onActivityCreated(Bundle savedInstanceState) {

        //TODO need to use String.Format to maintain decimal position

        stationNumber.setText("Station: " + station.getStationNumber());
        easting.setText(station.getStationEasting());
        northing.setText(station.getStationNorthing());

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                station.setStationNorthing(northing.getText().toString());
                station.setStationEasting(easting.getText().toString());
                callback.onDoneButtonClick(station);
            }
        });
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentStationUpdateOrCreateCallback) {
            callback = (FragmentStationUpdateOrCreateCallback) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement callback");
        }
    }

    public interface FragmentStationUpdateOrCreateCallback {
        void onDoneButtonClick(Station station);
    }

}

