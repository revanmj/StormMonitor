package pl.revanmj.stormmonitor;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.RemoteViews;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import pl.revanmj.stormmonitor.data.StormDataProvider;
import pl.revanmj.stormmonitor.logic.StormJob;
import pl.revanmj.stormmonitor.logic.Utils;
import pl.revanmj.stormmonitor.model.DownloadResult;
import pl.revanmj.stormmonitor.model.StormData;

import java.util.List;

/**
 * Created by revanmj on 26.12.2013.
 */

public class CitiesWidget extends AppWidgetProvider {
    private Context context;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        this.context = context;
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.cities_widget);

        Intent svcIntent = new Intent(context, WidgetService.class);
        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_listView, svcIntent);
        //views.setRemoteAdapter(appWidgetId, R.id.widget_listView, svcIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onEnabled(Context ctx) {
        StormJob.scheduleJob(ctx);
    }

    @Override
    public void onDisabled(Context ctx) {
        StormJob.cancelJob();
    }

}


