package com.caldera.geomapping.geomapping.services;

import android.content.Context;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import com.caldera.geomapping.geomapping.R;
import com.caldera.geomapping.geomapping.tasks.Deg2UTM;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class LocationResultHelper {

    public final static String KEY_LOCATION_UPDATES_RESULT = "location-update-result";

    private Context mContext;
    private Location location;
    private List<Location> mLocations;
    private double easting = 0.0;

    LocationResultHelper(Context context, List<Location> locations) {
        mContext = context;
        mLocations = locations;
    }

    /**
     * Returns the title for reporting about a list of {@link Location} objects.
     */
    private String getLocationResultTitle() {
        String numLocationsReported = mContext.getResources().getQuantityString(
                R.plurals.num_locations_reported, mLocations.size(), mLocations.size());
        return numLocationsReported + ": " + DateFormat.getDateTimeInstance().format(new Date());
    }

    private String getLocationResultText() {
        if (mLocations.isEmpty()) {
            return mContext.getString(R.string.unknown_location);
        }

        StringBuilder sb = new StringBuilder();

        for (Location location : mLocations) {
            double easting = new Deg2UTM(location).getEasting();
            double northing = new Deg2UTM(location).getNorthing();
            sb.append("(");
            sb.append(easting);
            sb.append(", ");
            sb.append(northing);
            sb.append(", ");
            sb.append(location.getAccuracy());
            sb.append(")");
            sb.append("\n");
        }
        Log.i("getLocationResultText", "location is: " + sb.toString());
        return sb.toString();
    }

    /**
     * Saves location result as a string to {@link android.content.SharedPreferences}.
     */
    void saveResults() {
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .edit()
                .putString(KEY_LOCATION_UPDATES_RESULT, getLocationResultTitle() + "\n" +
                        getLocationResultText())
                .apply();
    }

    /**
     * Fetches location results from {@link android.content.SharedPreferences}.
     */
    public static String getSavedLocationResult(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_LOCATION_UPDATES_RESULT, "");
    }
}
