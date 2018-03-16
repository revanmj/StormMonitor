package pl.revanmj.stormmonitor;

/**
 * Created by revanmj on 26.12.2013.
 */

import android.content.Intent;
import android.widget.RemoteViewsService;

import pl.revanmj.stormmonitor.adapters.WidgetAdapter;

public class WidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return (new WidgetAdapter(this.getApplicationContext(), intent));
    }

}
