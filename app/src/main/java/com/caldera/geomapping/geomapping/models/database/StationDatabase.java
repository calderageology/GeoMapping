package com.caldera.geomapping.geomapping.models.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import com.caldera.geomapping.geomapping.models.objects.Station;
import com.google.gson.Gson;

import java.util.ArrayList;



/**
 * Created by Michael on 22/03/2017.
 */

public class StationDatabase {

    /**
     * These variables describe what our Database will look like, what the table will be called,
     * and what each column will be called. See the nested SQLiteOpenHelper class below to see them
     * in action. These variables may also be placed in a seperate "contract" class, as described in
     * the Android Developer guide "Saving Data in SQL Databases"
     * @see <a href="https://developer.android.com/training/basics/data-storage/databases.html#DbHelper"</a>
     */

    private static final String TABLE_NAME = "stations";
    private static final String COLUMN_ENTRY_ID = "entry_id";
    private static final String COLUMN_STATION_NUMBER = "station_number";
    private static final String COLUMN_EASTING = "easting";
    private static final String COLUMN_NORTHING = "northing";
    private static final String COLUMN_STATION_DESCRIPTION = "description";
    private static final String COLUMN_STATION_DATA = "data";

    /**
     * - DATABASE_VERSION is to be incremented up by 1 (we'll, I think it just needs to be a larger
     * number...but I don't see the point in testing that theory), each time we change the schema,
     * or structure of the Database.
     * - DATABASE_NAME is simply the file name of our Database. We can use the file name in our
     * Java code to check if a Databse exists or not. This will come in handy if you want to
     * "pre-load" data into your database.
     */

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "stations.db";
    private static final String TEXT = " TEXT";
    private static final int INTEGER = 1;
    private static final String COMMA_SEP = ",";

    private StationDatabaseHelper helper;
    private static StationDatabase database;



    public static StationDatabase getInstance(Context c){

        /**
         * singleton method, prevents multiple instances of the database
         */

        if(database == null){
            database = new StationDatabase(c);
        }
        return database;
    }


    private StationDatabase(Context c){
        helper = new StationDatabaseHelper(c);
    }

    public Station getLastStation(){
        SQLiteDatabase db = helper.getReadableDatabase();
        Station result = null;
        String selectQuery = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ENTRY_ID + " DESC LIMIT 1";
        Gson gson = new Gson();

        Cursor c = db.rawQuery(selectQuery, null);


        if(c.moveToFirst()){
                Station station = gson.fromJson(c.getString(c.getColumnIndex(COLUMN_ENTRY_ID)), Station.class);
            result = station;
        }

            c.close();
            db.close();

        return result;


    }
    /**
     * Method which grabs all of the station data in our database.
     *
     * @return
     */

    public ArrayList getAllData(){
        SQLiteDatabase db = helper.getReadableDatabase();
        ArrayList<Station> result = new ArrayList<>();
        Gson gson = new Gson();

        Cursor c = db.query(TABLE_NAME, null, null, null, null, null, null);

        if(c.moveToFirst()){
            do {
                Station station = gson.fromJson(
                        c.getString(
                                c.getColumnIndex(COLUMN_STATION_DATA)
                        ),
                        Station.class);
                result.add(station);
            }
            while (c.moveToNext());
        }

        c.close();
        db.close();

        return result;
    }

    public long insertOrUpdateData(Station station){
        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor c = db.query(TABLE_NAME, null, null, null, null, null, null);

        ContentValues val = new ContentValues();
        Gson gson = new Gson();

        if(c.moveToFirst()){
            do {

                /** In English: If the current Cursor'd item Station Number matches the station number of the
                 * Station object we are trying to write, update that entry instead of creating a new
                 * one.
                 */

                if (c.getString(c.getColumnIndex(COLUMN_STATION_NUMBER))
                        .equals(station.getStationNumber())) {


                    val.put(COLUMN_EASTING, station.getStationEasting());
                    val.put(COLUMN_NORTHING, station.getStationNorthing());
                    val.put(COLUMN_STATION_DESCRIPTION, station.getStationDescription());
                    val.put(COLUMN_STATION_DATA, gson.toJson(station, Station.class));

                    /**selection and selectionArgs simple tell our db which rows we
                     *want to update. Notice how selectionArgs is an array. We could supply
                     *multiple column ids if we wish to.
                     */

                    String selection = COLUMN_ENTRY_ID + " LIKE ?";
                    String[] selectionArgs = {
                            String.valueOf(c.getString(c.getColumnIndex(COLUMN_ENTRY_ID)))
                    };
                    long id = db.update(TABLE_NAME, val, selection, selectionArgs);
                    c.close();
                    db.close();
                    return id;
                }
            }
            while (c.moveToNext());

        }

        val.put(COLUMN_STATION_NUMBER, station.getStationNumber());
        val.put(COLUMN_EASTING, station.getStationEasting());
        val.put(COLUMN_NORTHING, station.getStationNorthing());
        val.put(COLUMN_STATION_DESCRIPTION, station.getStationDescription());
        val.put(COLUMN_STATION_DATA, gson.toJson(station, Station.class));

        long id = db.insert(TABLE_NAME, null, val);

        c.close();
        db.close();

        return id;
    }

    /*-------------------Database Helper--------------------------------*/

    private static class StationDatabaseHelper extends SQLiteOpenHelper {
        private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ENTRY_ID + " INTEGER PRIMARY KEY," +
                COLUMN_STATION_NUMBER + TEXT + COMMA_SEP +
                COLUMN_STATION_DESCRIPTION + TEXT + COMMA_SEP +
                COLUMN_EASTING + TEXT + COMMA_SEP +
                COLUMN_NORTHING + TEXT + COMMA_SEP +
                COLUMN_STATION_DATA + TEXT +
                " )";

        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        private Context context;

        /**
         * the null value passed is a CursorFactory object. CursorFactory is used when we wish
         * to pass in a custom sub-class of Cursor. When would we need to do that? I have no
         * ****ing idea...
         * @param context - Self Explanatory
         */

        public StationDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            this.context = context;
        }


        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }
    }

}
