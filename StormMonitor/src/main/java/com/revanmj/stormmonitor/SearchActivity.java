package com.revanmj.stormmonitor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.revanmj.StormMonitor;
import com.revanmj.stormmonitor.model.StormData;
import com.revanmj.stormmonitor.sql.CitiesAssetHelper;
import com.revanmj.stormmonitor.sql.StormOpenHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity implements TextWatcher {

    private ListView wyniki;
    private EditText pole;
    private SearchAdapter sAdapter;
    private Tracker t;
    List<StormData> cities;
    List<StormData> res;
    StormOpenHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        getSupportActionBar().setTitle(R.string.title_activity_search);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get tracker.
        t = ((StormMonitor) SearchActivity.this.getApplication()).getTracker(StormMonitor.TrackerName.GLOBAL_TRACKER);
        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());

        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName("Search")
                .putContentType("Screens")
                .putContentId("screen-3"));

        db = new StormOpenHelper(SearchActivity.this);
        cities = db.getAllCities();
        res = new ArrayList<>();
        sAdapter = new SearchAdapter(res, this);

        wyniki = (ListView)findViewById(R.id.list_search);
        wyniki.setAdapter(sAdapter);
        pole = (EditText)findViewById(R.id.editText);

        pole.setOnKeyListener(new View.OnKeyListener()
        {
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            doSearch();
                            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.toggleSoftInput(InputMethodManager.RESULT_UNCHANGED_HIDDEN, 0);
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });
        pole.addTextChangedListener(this);
    }

    public void doSearch() {

        String query = pole.getText().toString();
        if (query.toLowerCase().startsWith("ą") || query.toLowerCase().startsWith("ć")  || query.toLowerCase().startsWith("ę") || query.toLowerCase().startsWith("ł") || query.toLowerCase().startsWith("ń") || query.toLowerCase().startsWith("ó") || query.toLowerCase().startsWith("ś") || query.toLowerCase().startsWith("ż") || query.toLowerCase().startsWith("ź"))
            query = query.substring(1);
        final CitiesAssetHelper cities_db = new CitiesAssetHelper(this);
        final List<StormData> results = new ArrayList<>();
        Cursor cursor = cities_db.searchCity(query);
        if (cursor.moveToFirst()) {
            do {
                StormData city = new StormData();
                city.setMiasto_id(Integer.parseInt(cursor.getString(0)));
                city.setMiasto(cursor.getString(1));

                results.add(city);
            } while (cursor.moveToNext());
        }

        if (results.size() == 0) {
            Toast.makeText(SearchActivity.this, R.string.message_no_results, Toast.LENGTH_SHORT).show();
        } else {
            sAdapter.clear();
            sAdapter.addAll(results);
            sAdapter.notifyDataSetChanged();

            wyniki.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    StormData tmp = cities_db.getCity(results.get(position).getMiasto());
                    if (tmp != null) {
                        if (!cityExists(tmp.getMiasto_id())) {
                            db.addCity(tmp);
                            Answers.getInstance().logContentView(new ContentViewEvent()
                                    .putContentName("Added city from list")
                                    .putContentType("Events")
                                    .putContentId("event-1"));
                            finish();
                        } else {
                            Toast.makeText(SearchActivity.this, R.string.message_city_exists, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(SearchActivity.this, R.string.message_no_such_city, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void addLocationCity(String data) {
        CitiesAssetHelper cities_db = new CitiesAssetHelper(this);
        StormData tmp = cities_db.getCity(data);
        if (tmp != null) {
            if (!cityExists(tmp.getMiasto_id())) {
                db.addCity(tmp);
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Added city via GPS")
                        .putContentType("Events")
                        .putContentId("event-2"));
                finish();
            } else {
                Toast.makeText(this, R.string.message_city_exists, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.message_no_such_city, Toast.LENGTH_SHORT).show();
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("Adding via GPS - City not found")
                    .putContentType("Events")
                    .putContentId("event-3"));
        }
    }

    private boolean cityExists(int cityId) {
        for (int i = 0; i < cities.size(); i++)
            if (cities.get(i).getMiasto_id() == cityId)
                return true;
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_gps:
                CityAsyncTask t = new CityAsyncTask(this);
                t.execute();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        doSearch();
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    public void sendGpsUsedEvent() {
        t.send(new HitBuilders.EventBuilder()
                .setCategory("Function")
                .setAction("Used adding by GPS")
                .setValue(1)
                .build());
    }

    public class CityAsyncTask extends AsyncTask<String, String, String> {
        Activity act;
        double latitude;
        double longitude;
        protected ProgressDialog postep;

        public CityAsyncTask(Activity act) {
            this.act = act;
        }

        @Override
        protected String doInBackground(String... params) {
            String result = "";
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, false);
            Location location = locationManager.getLastKnownLocation(provider);
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            Geocoder geocoder = new Geocoder(act, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude,
                        longitude, 1);
                Log.e("Addresses", "-->" + addresses);
                Address tmp = addresses.get(0);
                result = tmp.getLocality();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            postep.dismiss();
            addLocationCity(result);
            sendGpsUsedEvent();
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            postep = ProgressDialog.show(SearchActivity.this, null, "Trwa ustalanie lokalizacji ...", true, false);
        }
    }

}
