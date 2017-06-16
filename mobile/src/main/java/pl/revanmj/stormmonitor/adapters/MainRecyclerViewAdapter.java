package pl.revanmj.stormmonitor.adapters;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.data.StormDataProvider;
import pl.revanmj.stormmonitor.logic.Utils;

/**
 * Created by revanmj on 05.02.2016.
 */

public class MainRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int EMPTY_VIEW = 10;
    private Cursor mCursor;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == EMPTY_VIEW) {
            View itemView = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.empty_view, parent, false);

            return new EmptyViewHolder(itemView);
        }

        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.row_listview, parent, false);

        return new StormDataViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof StormDataViewHolder) {
            Cursor item = getItem(position);
            int id = item.getInt(StormDataProvider.CITYID);
            int stormTime = item.getInt(StormDataProvider.STORMTIME);
            int rainTime = item.getInt(StormDataProvider.RAINTIME);
            int stormChance = item.getInt(StormDataProvider.STORMCHANCE);
            int rainChance = item.getInt(StormDataProvider.RAINCHANCE);
            int stormAlert = item.getInt(StormDataProvider.STORMALERT);
            int rainAlert = item.getInt(StormDataProvider.RAINALERT);
            String city = item.getString(StormDataProvider.CITYNAME);

            ((StormDataViewHolder)holder).bind(id, city, stormChance, rainChance, stormTime, rainTime, stormAlert, rainAlert);
        }
    }

    @Override
    public int getItemCount() {
        if (mCursor != null && mCursor.getCount() > 0)
            return mCursor.getCount();
        else
            return 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (mCursor == null || mCursor.getCount() == 0) {
            return EMPTY_VIEW;
        }
        return super.getItemViewType(position);
    }

    public Cursor getItem(int pos) {
        if (this.mCursor != null && !this.mCursor.isClosed()) {
            this.mCursor.moveToPosition(pos);
        }
        return this.mCursor;
    }

    public void swapCursor(Cursor c) {
        mCursor = c;
        notifyDataSetChanged();
    }

    public static class StormDataViewHolder extends RecyclerView.ViewHolder {
        private int _id;
        private TextView city;
        private TextView stormChanceLabel;
        private TextView stormTimeLabel;
        private TextView rainTimeLabel;
        private TextView rainChanceLabel;
        private ImageView rect;

        public StormDataViewHolder(View itemView) {
            super(itemView);

            city = (TextView) itemView.findViewById(R.id.cityText);
            stormChanceLabel = (TextView) itemView.findViewById(R.id.stormChanceText);
            stormTimeLabel = (TextView) itemView.findViewById(R.id.stormTimeText);
            rect = (ImageView) itemView.findViewById(R.id.colorRectangle);
            rainChanceLabel = (TextView) itemView.findViewById(R.id.rainChanceText);
            rainTimeLabel = (TextView) itemView.findViewById(R.id.rainTimeText);
        }

        public void bind(int id, String city, int stormChance, int rainChance, int stormTime, int rainTime,
                         int stormAlert, int rainAlert) {
            this._id = id;
            this.city.setText(city);
            this.stormChanceLabel.setText(Integer.toString(stormChance));
            this.rainChanceLabel.setText(Integer.toString(rainChance));
            this.stormTimeLabel.setText(Utils.getTimeString(stormTime, stormAlert));
            this.rainTimeLabel.setText(Utils.getTimeString(rainTime, rainAlert));
            this.rect.setImageResource(Utils.getRectColor(stormTime, stormChance, rainTime, rainChance));
        }

        public int getId() {
            return this._id;
        }
    }

    public class EmptyViewHolder extends RecyclerView.ViewHolder {
        public EmptyViewHolder(View itemView) {
            super(itemView);
        }
    }
}
