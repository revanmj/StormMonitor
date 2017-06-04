package pl.revanmj.stormmonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.CurvedChildLayoutManager;
import android.support.wearable.view.WearableRecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import pl.revanmj.stormmonitor.data.CitiesAssetHelper;
import pl.revanmj.stormmonitor.data.StormData;
import pl.revanmj.stormmonitor.data.StormDataProvider;

public class AddActivity extends WearableActivity {
    private SearchAdapter searchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        searchAdapter = new SearchAdapter(this);
        WearableRecyclerView mListView = (WearableRecyclerView) findViewById(R.id.listView);
        mListView.setAdapter(searchAdapter);
        mListView.setCenterEdgeItems(true);
        CurvedChildLayoutManager mChildLayoutManager = new CurvedChildLayoutManager(this);
        mListView.setLayoutManager(mChildLayoutManager);

        loadData();
    }

    public void loadData() {
        // Prepare database
        final CitiesAssetHelper cities_db = new CitiesAssetHelper(this);
        final List<StormData> results = new ArrayList<>();

        // Get the results
        Cursor cursor = cities_db.getAllCities();
        if (cursor.moveToFirst()) {
            do {
                StormData city = new StormData();
                city.setCityId(Integer.parseInt(cursor.getString(0)));
                city.setCityName(cursor.getString(1));

                results.add(city);
            } while (cursor.moveToNext());
        }

        // Clear the ListView and add results to it
        searchAdapter.clear();
        searchAdapter.addAll(results);
        searchAdapter.notifyDataSetChanged();
    }

    private class SearchAdapter extends WearableRecyclerView.Adapter<WearableRecyclerView.ViewHolder> {
        private final LayoutInflater mInflater;
        private final ArrayList<StormData> listItems = new ArrayList<>();

        private SearchAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public WearableRecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new SearchViewHolder(mInflater.inflate(R.layout.row_city, null));
        }

        @Override
        public void onBindViewHolder(WearableRecyclerView.ViewHolder holder, int position) {
            TextView view = (TextView) holder.itemView.findViewById(R.id.text1);

            view.setText(listItems.get(position).getCityName());
            view.setTag(listItems.get(position).getCityId());
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TextView cityName = (TextView) view;
                    if (!Utils.cityExists((int) cityName.getTag(), AddActivity.this)) {
                        ContentValues cv = new ContentValues();
                        cv.put(StormDataProvider.KEY_ID, (int) cityName.getTag());
                        cv.put(StormDataProvider.KEY_CITYNAME, cityName.getText().toString());
                        getContentResolver().insert(StormDataProvider.CONTENT_URI, cv);

                        finish();
                    } else {
                        Toast.makeText(AddActivity.this, R.string.message_city_exists, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return listItems.size();
        }

        public void addAll(List<StormData> list) {
            listItems.addAll(list);
        }

        public void clear() {
            listItems.clear();
        }
    }

    class SearchViewHolder extends WearableRecyclerView.ViewHolder {

        public SearchViewHolder(View itemView) {
            super(itemView);
        }
    }
}
