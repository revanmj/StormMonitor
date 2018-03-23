package pl.revanmj.stormmonitor.adapters;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.logic.Utils;
import pl.revanmj.stormmonitor.data.StormData;

/**
 * Created by revanmj on 05.02.2016.
 */

public class StormRcAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
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
            ((StormDataViewHolder)holder).bind(Utils.getCityFromCursor(getItem(position)));
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

            city = itemView.findViewById(R.id.cityText);
            stormChanceLabel = itemView.findViewById(R.id.stormChanceText);
            stormTimeLabel = itemView.findViewById(R.id.stormTimeText);
            rect = itemView.findViewById(R.id.colorRectangle);
            rainChanceLabel = itemView.findViewById(R.id.rainChanceText);
            rainTimeLabel = itemView.findViewById(R.id.rainTimeText);
        }

        public void bind(StormData data) {
            this._id = data.getCityId();
            this.city.setText(data.getCityName());
            this.stormChanceLabel.setText(Integer.toString(data.getStormChance()));
            this.rainChanceLabel.setText(Integer.toString(data.getRainChance()));
            this.stormTimeLabel.setText(Utils.getTimeString(data.getStormTime(), data.getStormAlert()));
            this.rainTimeLabel.setText(Utils.getTimeString(data.getRainTime(), data.getRainAlert()));

            this.rect.setImageResource(Utils.getRectColor(data.getStormTime(), data.getStormChance(),
                    data.getRainTime(), data.getRainChance()));
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
