package com.caldera.geomapping.geomapping.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


import com.caldera.geomapping.geomapping.R;
import com.caldera.geomapping.geomapping.adapters.StationAdapter;
import com.caldera.geomapping.geomapping.models.objects.Station;


import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class FragStationList extends Fragment {
    private static final String LIST_DATA = "LIST_DATA";

    private ArrayList<Station> listData;
    private FragmentItemClickCallback callback;
    private RecyclerView stationList;
    private Button addStation;



    public FragStationList() {
        // Required empty public constructor
    }

    public static FragStationList newInstance(ArrayList<Station> listData){
        FragStationList fragment = new FragStationList();
        Bundle args = new Bundle();
        args.putParcelableArrayList(LIST_DATA, listData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstaceState){
        super.onCreate(savedInstaceState);

        if(getArguments() != null){
            this.listData = getArguments().getParcelableArrayList(LIST_DATA);
        }

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_station_list, container, false);
        addStation = (Button) v.findViewById(R.id.btn_add_station);
        stationList = (RecyclerView) v.findViewById(R.id.lst_stations);

        enableAddStationButton();
        return v;
    }

    private void enableAddStationButton() {
        addStation.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                if(callback != null){
                    callback.onAddStationButtonClicked();
                }
            }
        });
    }

    public void onActivityCreated(Bundle savedInstaceState){
        StationAdapter adapter = new StationAdapter(listData, getActivity());
        stationList.setAdapter(adapter);
        adapter.setItemClickCallback(new StationAdapter.ItemClickCallback() {
            @Override
            public void onItemClick(int p) {
                callback.onListItemClicked(p);
            }
        });
        stationList.setLayoutManager(new LinearLayoutManager(getActivity()));
        super.onActivityCreated(savedInstaceState);
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        if (context instanceof FragmentItemClickCallback){
            callback = (FragmentItemClickCallback) context;
        }else{
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach(){
        super.onDetach();
        callback = null;
    }

    public interface FragmentItemClickCallback {
        void onListItemClicked(int position);
        void onAddStationButtonClicked();
    }





}
