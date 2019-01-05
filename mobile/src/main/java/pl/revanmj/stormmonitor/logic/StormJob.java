package pl.revanmj.stormmonitor.logic;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import java.util.List;
import java.util.concurrent.TimeUnit;

import pl.revanmj.stormmonitor.CitiesWidget;
import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.data.SharedSettings;
import pl.revanmj.stormmonitor.data.StormData;

/**
 * Created by revanmj on 06.11.2016.
 */

public class StormJob extends Job {
    static final String TAG = "StormJob";

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        List<StormData> cities = Utils.loadCitiesFromDb(getContext());
        int result = Utils.downloadStormData(cities, getContext());
        Log.d(TAG, "Job finished with result: " + result);

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
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        int period = Integer.parseInt(settings.getString(SharedSettings.SYNC_PERIOD, "60"));
        Log.d(TAG, "Scheduled job with period: " + period);
        new JobRequest.Builder(StormJob.TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(period), TimeUnit.MINUTES.toMillis(5))
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    public static void scheduleJob(Integer period) {
        Log.d(TAG, "Scheduled job with period: " + period);
        new JobRequest.Builder(StormJob.TAG)
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
        Log.d(TAG, "Cancelled job with id: " + jobId);
    }
}
