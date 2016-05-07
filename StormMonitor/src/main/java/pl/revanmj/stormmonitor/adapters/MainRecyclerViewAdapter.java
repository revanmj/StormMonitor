package pl.revanmj.stormmonitor.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;

import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.data.StormDataProvider;
import pl.revanmj.stormmonitor.logic.Utils;

/**
 * Created by revanmj on 05.02.2016.
 */

public class MainRecyclerViewAdapter extends RecyclerView.Adapter<MainRecyclerViewAdapter.StormDataViewHolder> {
    private Context context;
    private Cursor cursor;

    public MainRecyclerViewAdapter(Context c) {
        context = c;
    }

    public void swapCursor(Cursor c) {
        cursor = c;
        notifyDataSetChanged();
    }

    @Override
    public StormDataViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.row_listview, parent, false);

        return new StormDataViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(StormDataViewHolder holder, int position) {
        Cursor item = getItem(position);
        int stormTime = item.getInt(StormDataProvider.STORMTIME);
        int rainTime = item.getInt(StormDataProvider.RAINTIME);
        int stormChance = item.getInt(StormDataProvider.STORMCHANCE);
        int rainChance = item.getInt(StormDataProvider.RAINCHANCE);

        holder._id = item.getInt(StormDataProvider.CITYID);
        holder.city.setText(item.getString(StormDataProvider.CITYNAME));
        holder.stormChanceLabel.setText(Integer.toString(stormChance));
        holder.rainChanceLabel.setText(Integer.toString(rainChance));
        holder.stormTimeLabel.setText(Utils.getTimeString(stormTime, item.getInt(StormDataProvider.STORMALERT)));
        holder.rainTimeLabel.setText(Utils.getTimeString(rainTime, item.getInt(StormDataProvider.RAINALERT)));
        holder.rect.setImageResource(Utils.getRectColor(stormTime, stormChance, rainTime, rainChance));

    }

    @Override
    public int getItemCount() {
        if (cursor != null)
            return cursor.getCount();
        else
            return 0;
    }

    public Cursor getItem(int pos) {
        if (this.cursor != null && !this.cursor.isClosed())
        {
            this.cursor.moveToPosition(pos);
        }

        return this.cursor;
    }

    public static class StormDataViewHolder extends RecyclerView.ViewHolder {
        public int _id;
        protected TextView city;
        protected TextView stormChanceLabel;
        protected TextView stormTimeLabel;
        protected TextView timeN;
        protected TextView rainTimeLabel;
        protected TextView rainTimeN;
        protected TextView rainChanceLabel;
        protected ImageView rect;

        public StormDataViewHolder(View itemView) {
            super(itemView);

            city = (TextView) itemView.findViewById(R.id.cityText);
            stormChanceLabel = (TextView) itemView.findViewById(R.id.chanceText);
            stormTimeLabel = (TextView) itemView.findViewById(R.id.timeText);
            timeN = (TextView) itemView.findViewById(R.id.textView3);
            rect = (ImageView) itemView.findViewById(R.id.colorRectangle);
            rainChanceLabel = (TextView) itemView.findViewById(R.id.rainChance);
            rainTimeLabel = (TextView) itemView.findViewById(R.id.rainTime);
            rainTimeN = (TextView) itemView.findViewById(R.id.textView4);
        }
    }
}
