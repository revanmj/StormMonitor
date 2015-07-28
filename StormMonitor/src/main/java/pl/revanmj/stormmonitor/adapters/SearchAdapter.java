package pl.revanmj.stormmonitor.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.model.StormData;

import java.util.List;

/**
 * Created by revanmj on 29.12.2013.
 */
public class SearchAdapter extends ArrayAdapter<StormData> {

    private List<StormData> cityList;
    private Context context;

    public SearchAdapter(List<StormData> cityList, Context ctx) {
        super(ctx, R.layout.row_search, cityList);
        this.cityList = cityList;
        this.context = ctx;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        StormData d = cityList.get(position);
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row_search, parent, false);

            holder = new ViewHolder();
            holder.city = (TextView) convertView.findViewById(R.id.city_search);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        TextView city = holder.city;

        city.setText(d.getMiasto());

        return convertView;
    }

    private static class ViewHolder {
        public TextView city;

    }
}
