package pl.revanmj.stormmonitor;

/**
 * Created by revanmj on 26.12.2013.
 */

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.widget.RemoteViewsService;

import pl.revanmj.stormmonitor.adapters.WidgetListAdapter;

public class WidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        return (new WidgetListAdapter(this.getApplicationContext(), intent));
    }

}
