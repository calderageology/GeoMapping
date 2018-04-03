package com.caldera.geomapping.geomapping.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.caldera.geomapping.geomapping.R;
import com.caldera.geomapping.geomapping.models.objects.Station;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Michael on 22/03/2017.
 */

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.CustomViewHolder> {
    private LayoutInflater inflater;
    private List<Station> listData;

    private ItemClickCallback itemClickCallback;

    public StationAdapter (List<Station> listData, Context c){
        inflater = LayoutInflater.from(c);
        this.listData = listData;
    }

    public interface ItemClickCallback {
        void onItemClick(int p);
    }

    public void setItemClickCallback(final ItemClickCallback itemClickCallback) {
        this.itemClickCallback = itemClickCallback;
    }

    @Override
    public StationAdapter.CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_station, parent, false);
        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CustomViewHolder holder, int position) {
        Station station = listData.get(position);
        holder.stationNumber.setText(station.getStationNumber());
        holder.stationDescription.setText(station.getStationDescription());
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    public void setListData(ArrayList<Station> stationList) {
        this.listData.clear();
        this.listData.addAll(stationList);
    }

    class CustomViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        View container;
        TextView stationDescription, stationNumber;

        public CustomViewHolder(View itemView) {
            super(itemView);
            stationNumber = (TextView) itemView.findViewById(R.id.lbl_list_item_number);
            stationDescription = (TextView) itemView.findViewById(R.id.lbl_list_item_desc);


            container = itemView.findViewById(R.id.cont_station_root);


            container.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            itemClickCallback.onItemClick(getAdapterPosition());
        }
    }
}