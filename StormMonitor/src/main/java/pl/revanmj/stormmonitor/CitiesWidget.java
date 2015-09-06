package pl.revanmj.stormmonitor;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.RemoteViews;

import pl.revanmj.stormmonitor.logic.Downloader;
import pl.revanmj.stormmonitor.model.DownloadResult;
import pl.revanmj.stormmonitor.model.StormData;
import pl.revanmj.stormmonitor.sql.StormOpenHelper;

import java.util.List;

public class CitiesWidget extends AppWidgetProvider {
    private StormOpenHelper db;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        db = new StormOpenHelper(context);
        RefreshData();
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
        views.setRemoteAdapter(appWidgetId, R.id.widget_listView, svcIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void setData(DownloadResult result)
    {
        if (result.getResultCode() == 1)
            for (StormData city : result.getCitiesData()) {
                db.updateCity(city);
            }
    }

    private class JSONStormTask extends AsyncTask<List<StormData>, Void, DownloadResult> {

        @Override
        protected DownloadResult doInBackground(List<StormData>... params) {
            DownloadResult result = null;

            if (params[0] != null) {
                // We list of the cities so download process can be started
                result = Downloader.getStormData(params[0]);
            }

            return result;
        }

        @Override
        protected void onPostExecute(DownloadResult result) {
            setData(result);
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }
    }

    public void RefreshData() {
        List<StormData> cities = db.getAllCities();
        JSONStormTask task = new JSONStormTask();
        task.execute(cities);
    }
}


