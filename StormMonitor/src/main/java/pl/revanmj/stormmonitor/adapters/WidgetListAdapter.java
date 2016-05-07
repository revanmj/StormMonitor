package pl.revanmj.stormmonitor.adapters;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.logic.Utils;
import pl.revanmj.stormmonitor.model.StormData;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by revanmj on 26.12.2013.
 */

public class WidgetListAdapter implements RemoteViewsService.RemoteViewsFactory {
    private List<StormData> cities;
    private Context context = null;
    private int appWidgetId;

    public WidgetListAdapter(Context context, Intent intent) {
        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        populateListItem();
    }

    private void populateListItem() {
        cities = Utils.getAllData(context);
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

        int t = listItem.getStormTime();
        int t_r = listItem.getRainTime();
        int ch = listItem.getStormChance();
        int ch_r = listItem.getRainChance();

        remoteView.setTextViewText(R.id.widget_cityText, listItem.getCityName());
        remoteView.setTextViewText(R.id.widget_chanceText, Integer.toString(ch));
        remoteView.setTextViewText(R.id.widget_rainChance, Integer.toString(ch_r));
        remoteView.setTextViewText(R.id.widget_timeText, Utils.getTimeString(t, listItem.getStormAlert()));
        remoteView.setTextViewText(R.id.widget_rainTime, Utils.getTimeString(t_r, listItem.getRainAlert()));
        remoteView.setImageViewResource(R.id.widget_colorRectangle, Utils.getRectColor(t, ch, t_r, ch_r));

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
