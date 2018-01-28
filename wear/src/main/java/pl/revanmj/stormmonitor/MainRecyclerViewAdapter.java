package pl.revanmj.stormmonitor;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import pl.revanmj.stormmonitor.data.StormDataProvider;

/**
 * Created by revanmj on 05.02.2016.
 */

public class MainRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int EMPTY_VIEW = 10;
    private Cursor mCursor;
    private Context mContext;

    public MainRecyclerViewAdapter(Context context) {
        mContext = context;
    }

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

    private Cursor getItem(int pos) {
        if (this.mCursor != null && !this.mCursor.isClosed()) {
            this.mCursor.moveToPosition(pos);
        }
        return this.mCursor;
    }

    public void swapCursor(Cursor c) {
        mCursor = c;
        notifyDataSetChanged();
    }

    class StormDataViewHolder extends RecyclerView.ViewHolder {
        private int _id;
        private TextView city;
        private TextView stormChanceLabel;
        private TextView stormTimeLabel;
        private TextView rainTimeLabel;
        private TextView rainChanceLabel;
        private ImageView rect;

        StormDataViewHolder(View itemView) {
            super(itemView);

            city = itemView.findViewById(R.id.city_label);
            stormChanceLabel = itemView.findViewById(R.id.chance_value_label);
            stormTimeLabel = itemView.findViewById(R.id.time_value_label);
            rect = itemView.findViewById(R.id.color_rectangle);
            rainChanceLabel = itemView.findViewById(R.id.rchance_value_label);
            rainTimeLabel = itemView.findViewById(R.id.rtime_value_label);

            itemView.setOnLongClickListener(view -> {
                String selection = StormDataProvider.KEY_ID + " = ?";
                String[] selArgs = {Integer.toString(_id)};
                int count = mContext.getContentResolver().delete(StormDataProvider.CONTENT_URI, selection, selArgs);
                Log.d("MainRecyclerViewAdapter", "Deleted rows: " + count);
                Toast.makeText(mContext, "Deleted city", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        void bind(int id, String city, int stormChance, int rainChance, int stormTime, int rainTime,
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
        EmptyViewHolder(View itemView) {
            super(itemView);
        }
    }
}
