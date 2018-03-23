package pl.revanmj.stormmonitor.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.data.StormData;

import java.util.List;

/**
 * Created by revanmj on 29.12.2013.
 */

public class SearchAdapter extends ArrayAdapter<StormData> {

    private List<StormData> mCitiesList;
    private Context mContext;

    public SearchAdapter(List<StormData> cityList, Context ctx) {
        super(ctx, R.layout.row_search, cityList);
        this.mCitiesList = cityList;
        this.mContext = ctx;
    }

    @NonNull
    public View getView(int position, View convertView, ViewGroup parent) {
        StormData data = mCitiesList.get(position);
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row_search, parent, false);

            holder = new ViewHolder();
            holder.city = convertView.findViewById(R.id.city_search);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        TextView city = holder.city;
        city.setText(data.getCityName());

        return convertView;
    }

    private static class ViewHolder {
        TextView city;
    }
}
