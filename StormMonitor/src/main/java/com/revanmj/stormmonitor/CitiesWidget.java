package com.revanmj.stormmonitor;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.RemoteViews;

import com.revanmj.stormmonitor.logic.CheckConnection;
import com.revanmj.stormmonitor.logic.Downloader;
import com.revanmj.stormmonitor.logic.JSONparser;
import com.revanmj.stormmonitor.model.StormData;
import com.revanmj.stormmonitor.sql.StormOpenHelper;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of App Widget functionality.
 */
public class CitiesWidget extends AppWidgetProvider {
    private StormOpenHelper db;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        db = new StormOpenHelper(context);
        RefreshData();
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.cities_widget);

        //RemoteViews Service needed to provide adapter for ListView
        Intent svcIntent = new Intent(context, WidgetService.class);
        //passing app widget id to that RemoteViews Service
        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        //setting a unique Uri to the intent
        //don't know its purpose to me right now
        svcIntent.setData(Uri.parse(
                svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
        //setting adapter to listview of the widget
        views.setRemoteAdapter(appWidgetId, R.id.widget_listView,
                svcIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void setData(List<StormData> result)
    {
        for (int i = 0; i < result.size(); i++) {
            db.updateCity(result.get(i));
        }
    }

    private class JSONStormTask extends AsyncTask<List<StormData>, Void, List<StormData>> {

        @Override
        protected List<StormData> doInBackground(List<StormData>... params) {
            List<StormData> lista = new ArrayList<StormData>();

            if (params[0] != null) {
                int ile = params[0].size();

                for (int i = 0; i < ile; i++) {
                    StormData stormData = new StormData();
                    String data = ( (new Downloader()).getStormData(params[0].get(i).getMiasto_id()));

                    if (data != null) {
                        try {
                            stormData = JSONparser.getStormData(data);
                            stormData.setMiasto_id(params[0].get(i).getMiasto_id());
                            lista.add(stormData);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        stormData.setP_burzy(0);
                        stormData.setT_burzy(500);
                        stormData.setMiasto("Pobieranie nie powiodło się!");
                        stormData.setError(true);
                    }
                }
            }

            return lista;
        }

        @Override
        protected void onPostExecute(List<StormData> result) {
            setData(result);
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }
    }

    public void RefreshData() {
        List<StormData> cities = db.getAllCities();
        if (CheckConnection.isHttpsAvalable("http://antistorm.eu/")) {
            JSONStormTask task = new JSONStormTask();
            task.execute(cities);
        }
    }
}


