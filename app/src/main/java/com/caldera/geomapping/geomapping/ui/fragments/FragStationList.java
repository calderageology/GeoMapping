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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


import com.caldera.geomapping.geomapping.R;
import com.caldera.geomapping.geomapping.adapters.StationAdapter;
import com.caldera.geomapping.geomapping.models.objects.Station;
import com.caldera.geomapping.geomapping.services.GPSService;


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


        if(!runtime_permissions()){
            enableAddStationButton();

            Log.d("onCreateView: if: ", "runtime permissions already set");

        }else {

            Log.d("onCreateView: if: ", "no runtime permissions");


        }

        return v;
    }

    private void enableAddStationButton() {
        Intent i = new Intent(getActivity(),GPSService.class);
        getActivity().startService(i);
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

    /**
     * request permissions from the user for location services
     * @return
     */
    public boolean runtime_permissions(){
        if(Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},100);
            Log.d("runtime_permissions: ", "required");
            return true;

        }
        Log.d("runtime_permissions: ", "not required");
        return false;
    }
    /**
     * user permissions for the GPS and network location services
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                Log.d("onRequestPermissionsResult: ", "granted");
               enableAddStationButton();
            }else{
                Log.d("onRequestPermissionsResult: if ", "not granted");
                runtime_permissions();
            }
        }
    }



}
