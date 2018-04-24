package com.caldera.geomapping.geomapping.models.objects;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * TODO: Add additional items to the station like UTM zone, datum, etc
 * Created by Michael on 19/02/2017.
 */

public class Station implements Parcelable {

    private String stationNumber, stationEasting, stationNorthing, stationElevation, stationTime, stationDescription;


    public Station(String stationNumber, String stationEasting, String stationNorthing,
                   String stationElevation, String stationTime, String stationDescription) {
        this.stationNumber = stationNumber;
        this.stationEasting = stationEasting;
        this.stationNorthing = stationNorthing;
        this.stationElevation = stationElevation;
        this.stationTime = stationTime;
        this.stationDescription = stationDescription;
    }

    protected Station(Parcel in) {
        stationNumber = in.readString();
        stationEasting = in.readString();
        stationNorthing = in.readString();
        stationElevation = in.readString();
        stationTime = in.readString();
        stationDescription = in.readString();

    }

    public static final Creator<Station> CREATOR = new Creator<Station>() {
        @Override
        public Station createFromParcel(Parcel in) {
            return new Station(in);
        }

        @Override
        public Station[] newArray(int size) {
            return new Station[size];
        }
    };

    public String getStationNumber() {
        return stationNumber;
    }

    public void setStationNumber(String stationNumber) {
        this.stationNumber = stationNumber;
    }

    public String getStationNorthing() {
        return stationNorthing;
    }

    public void setStationNorthing(String stationNorthing) {
        this.stationNorthing = stationNorthing;
    }

    public String getStationEasting() {

        return stationEasting;
    }

    public void setStationEasting(String stationEasting) {
        this.stationEasting = stationEasting;
    }

    public String getStationElevation() {

        return stationElevation;
    }

    public void setStationElevation (String stationEelevation) {
        this.stationElevation = stationElevation;
    }

    public String getStationTime (){
        return stationTime;
    }
    public void setStationTime (String stationTime) {
        this.stationTime = stationTime;
    }

    public String getStationDescription() {
        return stationDescription;
    }

    public void setStationDescription(String stationDescription) {
        this.stationDescription = stationDescription;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(stationNumber);
        dest.writeString(stationEasting);
        dest.writeString(stationNorthing);
        dest.writeString(stationElevation);
        dest.writeString(stationTime);
        dest.writeString(stationDescription);
    }



}