package pl.revanmj.stormmonitor;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import pl.revanmj.stormmonitor.model.StormData;
import pl.revanmj.stormmonitor.sql.StormOpenHelper;

import java.text.DecimalFormat;
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

    @Override
    public RemoteViews getViewAt(int position) {
        final RemoteViews remoteView = new RemoteViews(
                context.getPackageName(), R.layout.cities_widget_row);
        StormData listItem = cities.get(position);

        int t = listItem.getT_burzy();
        int t_r = listItem.getT_opadow();
        float chancePercentage = (listItem.getP_burzy() * 100.0f) / 255;
        float rainChancePercentage = (listItem.getP_opadow() * 100.0f) / 255;
        DecimalFormat form = new DecimalFormat("##.##");

        remoteView.setTextViewText(R.id.widget_cityText, listItem.getMiasto());
        remoteView.setTextViewText(R.id.widget_chanceText, form.format(chancePercentage) + " %");
        remoteView.setTextViewText(R.id.widget_rainChance, form.format(rainChancePercentage) + " %");

        if (t < 240) {
            remoteView.setTextViewText(R.id.widget_timeText, "~" + t + " min");
        } else {
            remoteView.setTextViewText(R.id.widget_timeText, "-");
        }

        if (t_r < 240) {
            remoteView.setTextViewText(R.id.widget_rainTime,"~ " + t_r + " min");
        } else {
            remoteView.setTextViewText(R.id.widget_rainTime, "-");
        }

        if (t <= 120 && chancePercentage > 30 && chancePercentage < 50 || t_r <= 120 && rainChancePercentage > 30 && rainChancePercentage < 50)
            remoteView.setImageViewResource(R.id.widget_colorRectangle, R.drawable.rectangle_yellow);
        else if (t <= 20 && chancePercentage >= 50 || t_r <= 20 && rainChancePercentage >= 50)
            remoteView.setImageViewResource(R.id.widget_colorRectangle, R.drawable.rectangle_red);
        else if (t <= 60 && chancePercentage > 30 || t_r <= 60 && rainChancePercentage > 30)
            remoteView.setImageViewResource(R.id.widget_colorRectangle, R.drawable.rectangle_orange);
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
