package com.caldera.geomapping.geomapping.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.caldera.geomapping.geomapping.R;
import com.caldera.geomapping.geomapping.models.objects.Station;

/**
 * A simple {@link Fragment} subclass. This fragment view may be useless.
 */
public class FragStationDetails extends Fragment {
    private static final String STATION = "STATION";

    private FragmentEditItemCallback callback;
    private TextView descriptionTitle, description, eastingTitle, easting, northingTitle, northing;
    private Button editStation;
    private Station station;


    public FragStationDetails() {
        // Required empty public constructor
    }

    public static FragStationDetails newInstance(Station station){
        FragStationDetails fragment = new FragStationDetails();
        Bundle args = new Bundle();
        args.putParcelable(STATION, station);
        fragment.setArguments(args);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            this.station = getArguments().getParcelable(STATION);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_station_details, container, false);

        descriptionTitle = (TextView) v.findViewById(R.id.lbl_station_desc_title);
        description = (TextView) v.findViewById(R.id.lbl_station_desc);
        eastingTitle = (TextView) v.findViewById(R.id.lbl_station_easting_title);
        easting = (TextView) v.findViewById(R.id.lbl_station_easting);
        northingTitle = (TextView) v.findViewById(R.id.lbl_station_northing_title);
        northing = (TextView) v.findViewById(R.id.lbl_station_northing);
        editStation = (Button) v.findViewById(R.id.btn_edit_station);

        return v;
    }

    public void onActivityCreated(Bundle savedInstanceState){
        description.setText(station.getStationDescription());
        easting.setText(station.getStationEasting());
        northing.setText(station.getStationNorthing());
        editStation.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                callback.onEditButtonClick(station);
            }
        });
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        if(context instanceof FragmentEditItemCallback){
            callback = (FragmentEditItemCallback) context;
        }else{
            throw new RuntimeException(context.toString() + " must implement callback");
        }
    }

    @Override
    public void onDetach(){
        super.onDetach();
        callback = null;
    }

    public interface FragmentEditItemCallback {
        void onEditButtonClick(Station station);
    }
}