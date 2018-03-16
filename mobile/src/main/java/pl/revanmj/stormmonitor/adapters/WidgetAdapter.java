package pl.revanmj.stormmonitor.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.logic.Utils;
import pl.revanmj.stormmonitor.model.StormData;

import java.util.List;

/**
 * Created by revanmj on 26.12.2013.
 */

public class WidgetAdapter implements RemoteViewsService.RemoteViewsFactory {
    private List<StormData> cities;
    private Context context = null;

    public WidgetAdapter(Context context, Intent intent) {
        this.context = context;
        populateListItem();
    }

    private void populateListItem() {
        cities = Utils.loadCitiesFromDb(context);
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

        int stormTime = listItem.getStormTime();
        int stormChance = listItem.getStormChance();
        int stormAlert = listItem.getStormAlert();

        int rainTime = listItem.getRainTime();
        int rainChance = listItem.getRainChance();
        int rainAlert = listItem.getRainAlert();

        remoteView.setTextViewText(R.id.widget_cityText, listItem.getCityName());
        remoteView.setTextViewText(R.id.widget_chanceText, Integer.toString(stormChance));
        remoteView.setTextViewText(R.id.widget_rainChance, Integer.toString(rainChance));
        remoteView.setTextViewText(R.id.widget_timeText, Utils.getTimeString(stormTime, stormAlert));
        remoteView.setTextViewText(R.id.widget_rainTime, Utils.getTimeString(rainTime, rainAlert));
        remoteView.setImageViewResource(R.id.widget_colorRectangle,
                Utils.getRectColor(stormTime, stormChance, rainTime, rainChance));

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
        final long token = Binder.clearCallingIdentity();
        try {
            populateListItem();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onDestroy() {
    }
}
