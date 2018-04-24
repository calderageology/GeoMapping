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
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import com.caldera.geomapping.geomapping.R;
import com.caldera.geomapping.geomapping.models.objects.Station;

public class FragStationUpdateOrCreate extends Fragment {
    private static final String STATION = "STATION";

    private Station station;
    private MultiAutoCompleteTextView description;
    private TextView stationNumber, easting, northing, time, elevation;
    private Button done;


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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.station = getArguments().getParcelable(STATION);
        } else {

            station = new Station("", "", "","","","");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);
/**
 * replace views
 */
        View v = inflater.inflate(R.layout.fragment_station_edit, container, false);
        stationNumber = (TextView) v.findViewById(R.id.lbl_edit_station_number);
        easting = (TextView) v.findViewById(R.id.lbl_edit_easting);
        northing = (TextView) v.findViewById(R.id.lbl_edit_northing);
        elevation = (TextView) v.findViewById(R.id.lbl_edit_elevation);
        time = (TextView) v.findViewById(R.id.lbl_edit_time);
        description = (MultiAutoCompleteTextView) v.findViewById(R.id.edt_edit_description);


        done = (Button) v.findViewById(R.id.btn_edit_done);

        return v;
    }

    /**
     * If station object exists (i.e. was passed in as an argument), update text fields with data
     * from existing station. Otherwise, create a new station object.
     * @param savedInstanceState
     */
    public void onActivityCreated(Bundle savedInstanceState) {

        //TODO need to use String.Format to maintain decimal position

        stationNumber.setText("Station Number: " + station.getStationNumber());
        easting.setText(station.getStationEasting() + "m E");
        northing.setText(station.getStationNorthing() + "m N");
        elevation.setText(station.getStationElevation() + "m ASL");
        time.setText(station.getStationTime());
        description.setText(station.getStationDescription());

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //    station.setStationNorthing(northing.getText().toString());
            //    station.setStationEasting(easting.getText().toString());
            //    station.setStationElevation(elevation.getText().toString());
            //    station.setStationTime(time.getText().toString());
                station.setStationDescription(description.getText().toString());
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

