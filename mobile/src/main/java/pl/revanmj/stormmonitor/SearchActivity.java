package pl.revanmj.stormmonitor;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import pl.revanmj.stormmonitor.adapters.SearchAdapter;
import pl.revanmj.stormmonitor.data.StormDataProvider;
import pl.revanmj.stormmonitor.logic.Utils;
import pl.revanmj.stormmonitor.model.StormData;
import pl.revanmj.stormmonitor.data.CitiesAssetHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by revanmj on 29.12.2013.
 */

public class SearchActivity extends AppCompatActivity {

    private static final String PERMISSION_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
    private ListView resultsListView;
    private EditText searchField;
    private SearchAdapter searchAdapter;
    private List<StormData> cities;
    private ProgressDialog locationLoading;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.md_blue_700));
        }

        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName("AddView")
                .putContentType("Views")
                .putContentId("addView"));

        // Get list of all cities
        cities = Utils.getAllData(this);

        // Prepare ListView
        List<StormData> results = new ArrayList<>();
        searchAdapter = new SearchAdapter(results, this);
        resultsListView = (ListView)findViewById(R.id.list_search);
        resultsListView.setAdapter(searchAdapter);
        searchField = (EditText)findViewById(R.id.search_text);

        // Add listener for Search key presses on virtual keyboard
        searchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                doSearch();
                final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.RESULT_UNCHANGED_HIDDEN, 0);
                return true;
            }
        });
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                doSearch();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    /**
     * Methods for cheking location permission in Android 6.0 and newer
     */
    private void checkForPermission() {
        int permission = ContextCompat.checkSelfPermission(this, PERMISSION_COARSE_LOCATION);

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_COARSE_LOCATION)) {
            Toast.makeText(this, R.string.error_location_denied, Toast.LENGTH_SHORT).show();
            return;
        }
        else if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{PERMISSION_COARSE_LOCATION}, 1);
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

    /**
     * Main method for searching the database
     */
    public void doSearch() {
        String query = searchField.getText().toString();

        // Remove polish letters
        if (query.toLowerCase().startsWith("ą") || query.toLowerCase().startsWith("ć")  ||
                query.toLowerCase().startsWith("ę") || query.toLowerCase().startsWith("ł") ||
                query.toLowerCase().startsWith("ń") || query.toLowerCase().startsWith("ó") ||
                query.toLowerCase().startsWith("ś") || query.toLowerCase().startsWith("ż") ||
                query.toLowerCase().startsWith("ź")) {
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
                city.setCityId(Integer.parseInt(cursor.getString(0)));
                city.setCityName(cursor.getString(1));

                results.add(city);
            } while (cursor.moveToNext());
        }

        if (results.size() == 0) {
            // No city fits the query
            Toast.makeText(SearchActivity.this, R.string.message_no_results, Toast.LENGTH_SHORT).show();
        } else {
            // Clear the ListView and add results to it
            searchAdapter.clear();
            searchAdapter.addAll(results);
            searchAdapter.notifyDataSetChanged();

            resultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // Add clicked city to the database
                    StormData tmp = cities_db.getCity(results.get(position).getCityName());
                    if (tmp != null) {
                        if (!cityExists(tmp.getCityId())) {
                            ContentValues cv = new ContentValues();
                            cv.put(StormDataProvider.KEY_ID, tmp.getCityId());
                            cv.put(StormDataProvider.KEY_CITYNAME, tmp.getCityName());
                            getContentResolver().insert(StormDataProvider.CONTENT_URI, cv);

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
     * Method for adding city via GPS without declaring AsyncTask objects all over the place
     */
    private void addCityByLocation() {
        try {
            locationLoading = ProgressDialog.show(SearchActivity.this, null,
                    "Trwa ustalanie lokalizacji ...", true, false);
            GoogleApiConnectionCallbacks connectionCallbacks = new GoogleApiConnectionCallbacks();
            googleApiClient = new GoogleApiClient.Builder(SearchActivity.this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(connectionCallbacks)
                    .addOnConnectionFailedListener(connectionCallbacks)
                    .build();
            googleApiClient.connect();
        } catch (SecurityException e) {}
    }

    /**
     * Method for adding a city from GPS data
     * @param data string that contains city name returned by Google Play services API
     */
    private void addLocationCity(String data) {
        CitiesAssetHelper cities_db = new CitiesAssetHelper(this);
        StormData tmp = cities_db.getCity(data);
        cities_db.close();

        if (tmp != null) {
            if (!cityExists(tmp.getCityId())) {
                ContentValues cv = new ContentValues();
                cv.put(StormDataProvider.KEY_ID, tmp.getCityId());
                cv.put(StormDataProvider.KEY_CITYNAME, tmp.getCityName());
                getContentResolver().insert(StormDataProvider.CONTENT_URI, cv);

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
            if (cities.get(i).getCityId() == cityId)
                return true;
        return false;
    }

    /**
     * Class for getting location data that's used for automatically adding city you're in now.
     * It returns a String with city's name returned by Google Play services API
     */
    class GoogleApiConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
            LocationListener {

        @Override
        public void onConnected(Bundle connectionHint) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

            if (location != null) {
                onLocationChanged(location);
            } else {
                Log.d("SearchActivity", "location is null!");
                LocationRequest request = new LocationRequest();
                request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, this);
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            //your code goes here
        }

        @Override
        public void onLocationChanged(Location location) {
            try {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Geocoder geocoder = new Geocoder(SearchActivity.this, Locale.getDefault());
                Address tmp = geocoder.getFromLocation(latitude, longitude, 1).get(0);

                String result = tmp.getLocality();
                if (result != null && !result.isEmpty())
                    addLocationCity(result);
                else
                    Toast.makeText(SearchActivity.this, R.string.no_location, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(SearchActivity.this, R.string.no_permission, Toast.LENGTH_SHORT).show();
            }
            locationLoading.dismiss();
            googleApiClient.disconnect();
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }
    }

}
