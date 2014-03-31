package com.revanmj.stormmonitor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.revanmj.stormmonitor.R;
import com.revanmj.stormmonitor.model.StormData;

import java.util.List;

/**
 * Created by revan_000 on 14.07.13.
 */
public class StormDataAdapter extends ArrayAdapter<StormData> {

    private List<StormData> cityList;
    private Context context;

    public StormDataAdapter(List<StormData> cityList, Context ctx) {
        super(ctx, R.layout.row_listview, cityList);
        this.cityList = cityList;
        this.context = ctx;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        StormData d = cityList.get(position);
        int t = d.getT_burzy();
        int t_r = d.getT_opadow();
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row_listview, parent, false);

            holder = new ViewHolder();
            holder.city = (TextView) convertView.findViewById(R.id.cityText);
            holder.chance = (TextView) convertView.findViewById(R.id.chanceText);
            holder.time = (TextView) convertView.findViewById(R.id.timeText);
            holder.timeN = (TextView) convertView.findViewById(R.id.textView3);
            holder.rect = (ImageView) convertView.findViewById(R.id.colorRectangle);
            holder.rainChance = (TextView) convertView.findViewById(R.id.rainChance);
            holder.rainTime = (TextView) convertView.findViewById(R.id.rainTime);
            holder.rainTimeN = (TextView) convertView.findViewById(R.id.textView4);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        TextView city = holder.city;
        TextView chance = holder.chance;
        TextView time = holder.time;
        TextView rainTime = holder.rainTime;
        TextView rainChance = holder.rainChance;
        ImageView rect = holder.rect;

        city.setText(d.getMiasto());
        chance.setText(d.getP_burzy() + " / 255");
        rainChance.setText(d.getP_opadow() + " / 255");

        if (t < 240) {
            time.setText("~ " + t + " min");
        } else {
            time.setText("-");
        }
        if (t_r < 240) {
            rainTime.setText("~ " + t_r + " min");
        } else {
            rainTime.setText("-");
        }
        if (t <= 120 && t > 60 && d.getP_burzy() > 30 || t_r <= 120 && t_r > 60 && d.getP_opadow() > 30)
            rect.setImageResource(R.drawable.rectangle_yellow);
        else if (t <= 60 && t > 20 || t_r <= 60 && t_r > 20)
            rect.setImageResource(R.drawable.rectangle_orange);
        else if (t <= 20 || t_r <= 20)
            rect.setImageResource(R.drawable.rectangle_red);
        else
            rect.setImageResource(R.drawable.rectangle_green);

        return convertView;
    }

    private static class ViewHolder {
        public TextView city;
        public TextView chance;
        public TextView time;
        public TextView timeN;
        public TextView rainTime;
        public TextView rainTimeN;
        public TextView rainChance;
        public ImageView rect;
    }

}
