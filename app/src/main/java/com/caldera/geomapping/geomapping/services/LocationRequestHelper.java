package com.caldera.geomapping.geomapping.services;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Not sure what this class does TODO: figure out the reason for this class
 */
public class LocationRequestHelper {

    public final static String KEY_LOCATION_UPDATES_REQUESTED = "location-updates-requested";

    public static void setRequesting(Context context, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_LOCATION_UPDATES_REQUESTED, value)
                .apply();
    }

    public static boolean getRequesting(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_LOCATION_UPDATES_REQUESTED, false);
    }
}
