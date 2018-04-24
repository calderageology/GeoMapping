package com.caldera.geomapping.geomapping.tasks;

import android.location.Location;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.ArrayList;
import java.util.List;

//TODO put Deg2UTM in here

public class GeoMath {

    private Location avgLocation = new Location("");
    private Location currentLocation;
    private double precision;



    public Location getAverageLocation(ArrayList<Location> locations){

        //unpack get the long, lat, and elevation from the arraylist
       List<Double> longitudes = new ArrayList<Double>();
       List<Double> latitudes = new ArrayList<Double>();
       List<Double> altitudes = new ArrayList<Double>();

        for (int i=0; i<=locations.size()-1; i++){
            longitudes.add(locations.get(i).getLongitude());
            latitudes.add(locations.get(i).getLatitude());
            altitudes.add(locations.get(i).getAltitude());
        }
        double longSum = 0;
        double latSum = 0;
        double altSum = 0;

        for(int j=0; j<=locations.size()-1; j++){
            longSum = longSum + longitudes.get(j);
            latSum = latSum + latitudes.get(j);
            altSum = altSum + altitudes.get(j);
        }
        double avgLongitude = longSum/longitudes.size();
        avgLocation.setLongitude(avgLongitude);
        double avgLatitude = latSum/latitudes.size();
        avgLocation.setLatitude(avgLatitude);
        double avgAltitude = altSum/altitudes.size();
        avgLocation.setAltitude(avgAltitude);

        return avgLocation;

    }

    public double getPrecision(ArrayList<Location> locations, Location location){
        getAverageLocation(locations);
        currentLocation = location;
        precision = currentLocation.distanceTo(avgLocation);
        return round(precision, 3);
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }



}
