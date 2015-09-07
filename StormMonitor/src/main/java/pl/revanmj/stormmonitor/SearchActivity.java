package pl.revanmj.stormmonitor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import pl.revanmj.stormmonitor.adapters.SearchAdapter;
import pl.revanmj.stormmonitor.model.StormData;
import pl.revanmj.stormmonitor.sql.CitiesAssetHelper;
import pl.revanmj.stormmonitor.sql.StormOpenHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.jar.Manifest;

public class SearchActivity extends AppCompatActivity implements TextWatcher {

    private ListView wyniki;
    private EditText pole;
    private SearchAdapter sAdapter;
    List<StormData> cities;
    List<StormData> res;
    StormOpenHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        getSupportActionBar().setTitle(R.string.title_activity_search);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName("AddView")
                .putContentType("Views")
                .putContentId("addView"));

        // Get list of all cities
        db = new StormOpenHelper(SearchActivity.this);
        cities = db.getAllCities();
        db.close();

        // Prepare ListView
        res = new ArrayList<>();
        sAdapter = new SearchAdapter(res, this);
        wyniki = (ListView)findViewById(R.id.list_search);
        wyniki.setAdapter(sAdapter);
        pole = (EditText)findViewById(R.id.editText);

        // Add listener for Search key presses on virtual keyboard
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

    /**
     * Main method for searching the database
     */
    public void doSearch() {
        String query = pole.getText().toString();

        // Remove polish letters
        if (query.toLowerCase().startsWith("ą") || query.toLowerCase().startsWith("ć")  || query.toLowerCase().startsWith("ę") || query.toLowerCase().startsWith("ł") || query.toLowerCase().startsWith("ń") || query.toLowerCase().startsWith("ó") || query.toLowerCase().startsWith("ś") || query.toLowerCase().startsWith("ż") || query.toLowerCase().startsWith("ź")) {
            query = query.substring(1);
        }

        // Prepare database
        final CitiesAssetHelper cities_db = new CitiesAssetHelper(this);
        final List<StormData> results = new ArrayList<>();

        // Get the results
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
            // No city fits the query
            Toast.makeText(SearchActivity.this, R.string.message_no_results, Toast.LENGTH_SHORT).show();
        } else {
            // Clear the ListView and add results to it
            sAdapter.clear();
            sAdapter.addAll(results);
            sAdapter.notifyDataSetChanged();

            wyniki.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // Add clicked city to the database
                    StormData tmp = cities_db.getCity(results.get(position).getMiasto());
                    if (tmp != null) {
                        if (!cityExists(tmp.getMiasto_id())) {
                            db.addCity(tmp);
                            Answers.getInstance().logContentView(new ContentViewEvent()
                                    .putContentName("AddView")
                                    .putContentType("Actions")
                                    .putContentId("addedCityFromList"));
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

    /**
     * Methods for cheking location permission in Android 6.0 and newer
     */
    private void checkForPermission() {
        int permission = ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.ACCESS_FINE_LOCATION"))
            Toast.makeText(this, R.string.error_location_denied, Toast.LENGTH_SHORT)
                    .show();
        else if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"},
                        1);
                return;
        }

        addCityByLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addCityByLocation();
                } else {
                    Toast.makeText(this, R.string.error_location_denied, Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    /**
     * Method for adding city via GPS without declaring AsyncTask objects all over the place
     */
    private void addCityByLocation() {
        CityAsyncTask t = new CityAsyncTask(this);
        t.execute();
    }

    /**
     * Method for adding a city from GPS data
     * @param data string that contains city name returned by Google Play services API
     */
    private void addLocationCity(String data) {
        CitiesAssetHelper cities_db = new CitiesAssetHelper(this);
        StormData tmp = cities_db.getCity(data);
        if (tmp != null) {
            if (!cityExists(tmp.getMiasto_id())) {
                db.addCity(tmp);
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("AddView")
                        .putContentType("Actions")
                        .putContentId("addedCityViaGPS"));
                finish();
            } else {
                Toast.makeText(this, R.string.message_city_exists, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.message_no_such_city, Toast.LENGTH_SHORT).show();
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("City not found: " + data)
                    .putContentType("Errors")
                    .putContentId("noSuchCity"));
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
                checkForPermission();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        // Initialize search on every keypress so results list is constantly updated
        doSearch();
    }

    @Override
    public void afterTextChanged(Editable editable) {}

    /**
     * Class for getting location data that's used for automatically adding city you're in now.
     * It returns a String with city's name returned by Google Play services API
     */
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
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            postep = ProgressDialog.show(SearchActivity.this, null, "Trwa ustalanie lokalizacji ...", true, false);
        }
    }

}
