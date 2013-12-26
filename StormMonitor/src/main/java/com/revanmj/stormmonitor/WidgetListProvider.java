package com.revanmj.stormmonitor;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.revanmj.stormmonitor.model.StormData;
import com.revanmj.stormmonitor.sql.StormOpenHelper;

import java.util.List;

/**
 * Created by revanmj on 26.12.2013.
 */
public class WidgetListProvider implements RemoteViewsService.RemoteViewsFactory {
    private List<StormData> cities;
    private StormOpenHelper db;
    private Context context = null;
    private int appWidgetId;

    public WidgetListProvider(Context context, Intent intent) {
        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        db = new StormOpenHelper(context);
        populateListItem();
    }

    private void populateListItem() {
        cities = db.getAllCities();
    }

    @Override
    public int getCount() {
        return cities.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /*
    *Similar to getView of Adapter where instead of View
    *we return RemoteViews
    *
    */
    @Override
    public RemoteViews getViewAt(int position) {
        final RemoteViews remoteView = new RemoteViews(
                context.getPackageName(), R.layout.cities_widget_row);
        StormData listItem = cities.get(position);
        Integer p = listItem.getP_burzy();
        Integer t = listItem.getT_burzy();
        remoteView.setTextViewText(R.id.widget_cityText, listItem.getMiasto());
        remoteView.setTextViewText(R.id.widget_chanceText, p  + " / 255");

        if (t < 240) {
            remoteView.setTextViewText(R.id.widget_timeText, "~" + t + " min");
            remoteView.setViewVisibility(R.id.widget_textView3, View.VISIBLE);
            remoteView.setViewVisibility(R.id.widget_timeText, View.VISIBLE);
        } else {
            remoteView.setViewVisibility(R.id.widget_textView3, View.INVISIBLE);
            remoteView.setViewVisibility(R.id.widget_timeText, View.INVISIBLE);
        }
        if (t <= 120 && t > 60 && listItem.getP_burzy() > 30)
            remoteView.setImageViewResource(R.id.widget_colorRectangle, R.drawable.rectangle_yellow);
        else if (t <= 60 && t > 20)
            remoteView.setImageViewResource(R.id.widget_colorRectangle, R.drawable.rectangle_orange);
        else if (t <= 20)
            remoteView.setImageViewResource(R.id.widget_colorRectangle, R.drawable.rectangle_red);
        else
            remoteView.setImageViewResource(R.id.widget_colorRectangle, R.drawable.rectangle_green);

        return remoteView;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
    }

    @Override
    public void onDestroy() {
    }
}
