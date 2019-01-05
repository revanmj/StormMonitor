package pl.revanmj.stormmonitor.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.adapters.SearchAdapter;
import pl.revanmj.stormmonitor.data.StormDataProvider;
import pl.revanmj.stormmonitor.logic.Utils;
import pl.revanmj.stormmonitor.data.StormData;
import pl.revanmj.stormmonitor.data.CitiesAssetHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by revanmj on 29.12.2013.
 */

public class AddCityActivity extends AppCompatActivity {
    private static final String LOG_TAG = AddCityActivity.class.getSimpleName();
    private ListView mResultsListView;
    private SearchAdapter mSearchAdapter;
    private List<StormData> mCities;
    private ProgressDialog mLocationLoadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_city);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName("AddView")
                .putContentType("Views")
                .putContentId("addView"));

        // Get list of all mCities
        mCities = Utils.loadCitiesFromDb(this);

        // Prepare ListView
        List<StormData> results = new ArrayList<>();
        mSearchAdapter = new SearchAdapter(results, this);
        mResultsListView = findViewById(R.id.list_search);
        mResultsListView.setAdapter(mSearchAdapter);
        searchCity("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchCity(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchCity(newText);
                return false;
            }
        });
        searchView.setIconified(false);
        searchView.onActionViewExpanded();
        searchView.requestFocusFromTouch();

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
     * Methods for cheking location permission in Android 6.0 and newer
     */
    private void checkForPermission() {
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Toast.makeText(this, R.string.error_location_denied, Toast.LENGTH_SHORT).show();
            return;
        }
        else if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }

        addCityByLocation();
    }

    /**
     * Main method for searching the database
     */
    public void searchCity(String query) {
        // Remove polish letters
        if (query.toLowerCase().startsWith("ą") || query.toLowerCase().startsWith("ć") ||
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
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                results.add(Utils.getCityFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        mSearchAdapter.clear();
        if (results.size() == 0) {
            // No city fits the query
            Toast.makeText(AddCityActivity.this, R.string.message_no_results, Toast.LENGTH_SHORT).show();
        } else {
            // Clear the ListView and add results to it
            mSearchAdapter.addAll(results);
            mSearchAdapter.notifyDataSetChanged();

            mResultsListView.setOnItemClickListener((parent, view, position, id) -> {
                // Add clicked city to the database
                StormData tmp = cities_db.getCity(results.get(position).getCityName());
                if (tmp != null) {
                    if (!containsCityId(tmp.getCityId())) {
                        ContentValues cv = new ContentValues();
                        cv.put(StormDataProvider.KEY_ID, tmp.getCityId());
                        cv.put(StormDataProvider.KEY_CITYNAME, tmp.getCityName());
                        getContentResolver().insert(StormDataProvider.CONTENT_URI, cv);

                        Answers.getInstance().logContentView(new ContentViewEvent()
                                .putContentName("AddView")
                                .putContentType("Actions")
                                .putContentId("addedCityFromList"));

                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(AddCityActivity.this, R.string.message_city_exists, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AddCityActivity.this, R.string.message_no_such_city, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Method for adding city via GPS without declaring AsyncTask objects all over the place
     */
    private void addCityByLocation() {
        try {
            FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
            mLocationLoadingDialog = new ProgressDialog(this);
            // Get the last known location
            client.getLastLocation()
                    .addOnCompleteListener(this, task -> {
                        Log.d(LOG_TAG, "MapMyLocationCallback - onLocationAquired");
                        Location location = task.getResult();
                        if (location != null) {
                            try {
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();
                                Geocoder geocoder = new Geocoder(AddCityActivity.this, Locale.getDefault());
                                Address tmp = geocoder.getFromLocation(latitude, longitude, 1).get(0);

                                String result = tmp.getLocality();
                                if (result != null && !result.isEmpty())
                                    addCityByName(result);
                                else
                                    Toast.makeText(AddCityActivity.this, R.string.no_location, Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Toast.makeText(AddCityActivity.this, R.string.no_permission, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(LOG_TAG, "MapMyLocationCallback - location is null!");
                            Toast.makeText(AddCityActivity.this, R.string.no_location, Toast.LENGTH_SHORT).show();
                        }

                        mLocationLoadingDialog.dismiss();
                    })
                    .addOnFailureListener(this, task -> {
                        mLocationLoadingDialog.dismiss();
                        Log.d(LOG_TAG, "getLastLocation failed");
                    });
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "addCityByLocation - no permission!");
        }
    }

    /**
     * Method for adding a city with name found from GPS data
     * @param data string that contains city name returned by Google Play services API
     */
    private void addCityByName(String data) {
        CitiesAssetHelper cities_db = new CitiesAssetHelper(this);
        StormData tmp = cities_db.getCity(data);
        cities_db.close();

        if (tmp != null) {
            if (!containsCityId(tmp.getCityId())) {
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

    private boolean containsCityId(int cityId) {
        for (int i = 0; i < mCities.size(); i++)
            if (mCities.get(i).getCityId() == cityId)
                return true;
        return false;
    }
}
