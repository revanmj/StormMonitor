package pl.revanmj.stormmonitor.logic;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import pl.revanmj.stormmonitor.CitiesWidget;
import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.data.SharedSettings;
import pl.revanmj.stormmonitor.data.StormDataProvider;
import pl.revanmj.stormmonitor.model.StormData;

/**
 * Created by revanmj on 06.11.2016.
 */

public class StormJob extends Job {

    public static final String TAG = "job_storm_tag";

    @Override
    protected Result onRunJob(Params params) {
        List<StormData> cities = Utils.getAllData(getContext());
        int result = Utils.getStormData(cities, getContext());
        Log.d("StormJob", "job finished with result: " + result);

        int[] widgetIDs = AppWidgetManager.getInstance(getContext().getApplicationContext())
                .getAppWidgetIds(new ComponentName(getContext().getApplicationContext(), CitiesWidget.class));

        for (int id : widgetIDs)
            AppWidgetManager.getInstance(getContext().getApplicationContext())
                    .notifyAppWidgetViewDataChanged(id, R.id.widget_listView);


        if (result == 1)
            return Result.SUCCESS;
        else
            return Result.FAILURE;
    }

    public static void scheduleJob(Context ctx) {
        SharedPreferences settings = ctx.getSharedPreferences(SharedSettings.FILE, Context.MODE_PRIVATE);
        int period = settings.getInt(SharedSettings.SYNC_PERIOD, 15);
        Log.d("StormJob", "scheduled job with period: " + period);
        new JobRequest.Builder(StormJob.TAG)
                .setPersisted(true)
                .setPeriodic(TimeUnit.MINUTES.toMillis(period), TimeUnit.MINUTES.toMillis(5))
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    public static void cancelJob() {
        int jobId = new JobRequest.Builder(StormJob.TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
                .setUpdateCurrent(true)
                .build()
                .schedule();

        JobManager.instance().cancel(jobId);
        Log.d("StormJob", "cancelled job with id: " + jobId);
    }
}
