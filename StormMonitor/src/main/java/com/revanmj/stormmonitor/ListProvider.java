package com.revanmj.stormmonitor;

/**
 * Created by revanmj on 23.11.2013.
 */
import java.util.ArrayList;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;
import android.widget.TextView;

import com.revanmj.stormmonitor.R;

/**
 * If you are familiar with Adapter of ListView,this is the same as adapter
 * with few changes
 *
 */
public class ListProvider implements RemoteViewsFactory {
    private ArrayList<Integer> miasta = new ArrayList<Integer>();
    private Context context = null;
    private int appWidgetId;

    public ListProvider(Context context, Intent intent) {
        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        populateListItem();
    }

    private void populateListItem() {
        //pobrać dane z pliku

    }

    @Override
    public int getCount() {
        return miasta.size();
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
                context.getPackageName(), R.layout.row_listview);
        Integer listItem = miasta.get(position);

        remoteView.setTextViewText(R.id.cityText, listItem.toString());
        remoteView.setImageViewResource(R.id.colorRectangle, R.drawable.rectangle_green);

//        if (convertView == null) {
//            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            convertView = inflater.inflate(R.layout.row_listview, parent, false);
//
//            holder = new ViewHolder();
//            holder.city = (TextView) remoteView.findViewById(R.id.cityText);
//            holder.chance = (TextView) convertView.findViewById(R.id.chanceText);
//            holder.time = (TextView) convertView.findViewById(R.id.timeText);
//            holder.timeN = (TextView) convertView.findViewById(R.id.textView3);
//            holder.rect = (SurfaceView) convertView.findViewById(R.id.colorRectangle);
//
//            convertView.setTag(holder);
//        } else {
//            holder = (ViewHolder) convertView.getTag();
//        }
//
//        TextView city = holder.city;
//        TextView chance = holder.chance;
//        TextView time = holder.time;
//        TextView timeN = holder.timeN;
//        SurfaceView rect = holder.rect;
//
//        city.setText(d.getMiasto());
//        chance.setText(d.getP_burzy() + " / 255");
//
//        if (t < 240) {
//            time.setText("~" + t + " min");
//            time.setVisibility(View.VISIBLE);
//            timeN.setVisibility(View.VISIBLE);
//        } else {
//            time.setVisibility(View.INVISIBLE);
//            timeN.setVisibility(View.INVISIBLE);
//        }
//        if (t <= 120 && t > 60 && d.getP_burzy() > 30)
//            rect.setBackgroundResource(R.drawable.rectangle_yellow);
//        else if (t <= 60 && t > 20)
//            rect.setBackgroundResource(R.drawable.rectangle_orange);
//        else if (t <= 20)
//            rect.setBackgroundResource(R.drawable.rectangle_red);
//        else
//            rect.setBackgroundResource(R.drawable.rectangle_green);


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