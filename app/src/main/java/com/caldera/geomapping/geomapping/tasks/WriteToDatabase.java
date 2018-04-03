package com.caldera.geomapping.geomapping.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.caldera.geomapping.geomapping.models.database.StationDatabase;
import com.caldera.geomapping.geomapping.models.objects.Station;

/**
 * Created by Michael on 22/03/2017.
 */

public class WriteToDatabase extends AsyncTask<Void, Void, Long> {
    private Station station;
    private Context context;

    private OnWriteComplete onWriteComplete;

    public interface OnWriteComplete {
        void setWriteComplete(long result);
    }

    public WriteToDatabase (Context context, Station station){
        this.station = station;
        this.context = context;
    }

    public void setWriteCompleteListener(OnWriteComplete onWriteComplete) {
        this.onWriteComplete = onWriteComplete;
    }

    @Override
    protected Long doInBackground(Void... params) {
        StationDatabase database = StationDatabase.getInstance(context);
        return database.insertOrUpdateData(station);
    }

    /**
     *If param ends up as -1, then our station has failed to be entered into the Database
     * @param param - Result of database operation
     */
    @Override
    protected void onPostExecute(Long param) {
        onWriteComplete.setWriteComplete(param);
    }
}
