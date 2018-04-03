package com.caldera.geomapping.geomapping.models.objects;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * this class is intended to represent data which we can expect to be associated with the To Do object
 * Created by Michael on 19/02/2017.
 */

public class Station implements Parcelable {

    private String stationNumber;
    private String stationDescription;
    private String stationEasting;
    private String stationNorthing;

    /**
     *
     * @param stationNumber - Description of the station
     * @param stationEasting - Easting of the station
     * @param stationNorthing - Northing of the station
     *
     * To add: longitude, latitude, zone, letterUTM, elevation, accuracy
     */

    public Station(String stationNumber, String stationEasting, String stationNorthing) {
        this.stationNumber = stationNumber;
        this.stationEasting = stationEasting;
        this.stationNorthing = stationNorthing;
    }

    protected Station(Parcel in) {
        stationNumber = in.readString();
        stationEasting = in.readString();
        stationNorthing = in.readString();
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

    public String getStationDescription() {
        return stationDescription;
    }

    public void setStationDescription(String stationDescription) {
        this.stationDescription = stationDescription;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(stationNumber);
        dest.writeString(stationEasting);
        dest.writeString(stationNorthing);
    }



}