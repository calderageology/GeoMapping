package com.caldera.geomapping.geomapping.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.caldera.geomapping.geomapping.models.database.StationDatabase;

import java.util.ArrayList;

/**
 * Created by Michael on 22/03/2017.
 */

public class ReadFromDatabase extends AsyncTask<Void, Void, ArrayList> {
    private Context context;

    public OnQueryComplete onQueryComplete;

    public interface OnQueryComplete {
        void setQueryComplete(ArrayList result);
    }

    public void setQueryCompleteListener(OnQueryComplete onQueryComplete){
        this.onQueryComplete = onQueryComplete;
    }

    public ReadFromDatabase(Context context){
        this.context = context;
    }


    @Override
    protected ArrayList doInBackground(Void... params) {
        StationDatabase database = StationDatabase.getInstance(context);
        return database.getAllData();
    }

    @Override
    protected void onPostExecute(ArrayList result){
        onQueryComplete.setQueryComplete(result);
    }
}
