package pl.revanmj.stormmonitor.logic;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import pl.revanmj.stormmonitor.CitiesWidget;
import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.data.SharedSettings;
import pl.revanmj.stormmonitor.data.StormData;

/**
 * Created by revanmj on 06.11.2016.
 */

public class StormWorker extends Worker {
    private static final String TAG = "StormWorker";
    private Context mContext;

    public StormWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        List<StormData> cities = Utils.loadCitiesFromDb(mContext);
        int result = Utils.downloadStormData(cities, mContext);
        Log.d(TAG, "Work finished with result: " + result);

        int[] widgetIDs = AppWidgetManager.getInstance(mContext.getApplicationContext())
                .getAppWidgetIds(new ComponentName(mContext.getApplicationContext(), CitiesWidget.class));

        for (int id : widgetIDs)
            AppWidgetManager.getInstance(mContext.getApplicationContext())
                    .notifyAppWidgetViewDataChanged(id, R.id.widget_listView);

        if (result == 1)
            return Result.success();
        else
            return Result.failure();
    }

    public static void scheduleJob(Context ctx) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        int period = Integer.parseInt(settings.getString(SharedSettings.SYNC_PERIOD, "60"));
        Log.d(TAG, "Scheduled work with period: " + period);
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest stormWorkRequest = new PeriodicWorkRequest.Builder(StormWorker.class, period, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance().enqueue(stormWorkRequest);
    }

    public static void scheduleJob(Integer period) {
        Log.d(TAG, "Scheduled work with period: " + period);
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest stormWorkRequest = new PeriodicWorkRequest.Builder(StormWorker.class, period, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance().enqueue(stormWorkRequest);
    }

    public static void cancelJob() {
        UUID jobId = new PeriodicWorkRequest.Builder(StormWorker.class, 15, TimeUnit.MINUTES)
                .build().getId();

        WorkManager.getInstance().cancelWorkById(jobId);
        Log.d(TAG, "Cancelled work with id: " + jobId);
    }
}
