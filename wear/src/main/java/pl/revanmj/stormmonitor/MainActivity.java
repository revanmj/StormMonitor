package pl.revanmj.stormmonitor;

import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.app.LoaderManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wear.widget.WearableRecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import pl.revanmj.stormmonitor.data.CitiesAssetHelper;
import pl.revanmj.stormmonitor.data.StormData;
import pl.revanmj.stormmonitor.data.StormDataProvider;

public class MainActivity extends WearableActivity implements MenuItem.OnMenuItemClickListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String PERMISSION_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
    private static final String KEY_LAST_UPDATE = "lastUpdate";

    private WearableRecyclerView recyclerView;
    private MainRecyclerViewAdapter rcAdapter;
    private ProgressBar loadingAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_launcher_view);
        loadingAnim = findViewById(R.id.progressBar);

        rcAdapter = new MainRecyclerViewAdapter(this);
        recyclerView.setAdapter(rcAdapter);
        recyclerView.setEdgeItemsCenteringEnabled(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        WearableActionDrawerView actionDrawer = findViewById(R.id.bottom_action_drawer);
        actionDrawer.setOnMenuItemClickListener(this);
        actionDrawer.getController().peekDrawer();

        getLoaderManager().initLoader(0, null, new StormCursorLoader());

        loadingAnim.setProgress(0);

        if (System.currentTimeMillis() - getPreferences(0).getLong(KEY_LAST_UPDATE, 0) > 900000) {
            refreshData();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        boolean done = false;

        switch (menuItem.getItemId()) {
            case R.id.menu_add_search:
                Intent intent = new Intent(this, AddActivity.class);
                startActivity(intent);
                done = true;
                break;
            case R.id.menu_add_gps:
                checkForPermission();
                done = true;
                break;
            case R.id.menu_refresh:
                refreshData();
                done = true;
                break;
        }
        WearableActionDrawerView actionDrawer = findViewById(R.id.bottom_action_drawer);
        actionDrawer.getController().closeDrawer();
        return done;
    }

    /**
     * Methods for cheking location permission in Android 6.0 and newer
     */
    private void checkForPermission() {
        int permission = ContextCompat.checkSelfPermission(this, PERMISSION_COARSE_LOCATION);

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_COARSE_LOCATION)) {
            Toast.makeText(this, R.string.no_permission, Toast.LENGTH_SHORT).show();
            return;
        }
        else if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{PERMISSION_COARSE_LOCATION}, 1);
            return;
        }

        addCityByLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addCityByLocation();
                } else {
                    Toast.makeText(this, R.string.no_permission, Toast.LENGTH_SHORT)
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
        try {
            FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
            loadingAnim.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            // Get the last known location
            client.getLastLocation()
                    .addOnCompleteListener(this, task -> {
                        Log.d(LOG_TAG, "MapMyLocationCallback - onLocationAquired");
                        Location location = task.getResult();
                        if (location != null) {
                            try {
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();
                                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                Address tmp = geocoder.getFromLocation(latitude, longitude, 1).get(0);

                                String result = tmp.getLocality();
                                if (result != null && !result.isEmpty())
                                    addLocationCity(result);
                                else
                                    Toast.makeText(MainActivity.this, R.string.no_location, Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Toast.makeText(MainActivity.this, R.string.no_permission, Toast.LENGTH_SHORT).show();
                            }
                        } else
                            Toast.makeText(MainActivity.this, R.string.no_location, Toast.LENGTH_SHORT).show();

                        loadingAnim.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    })
                    .addOnFailureListener(this, task -> {
                        loadingAnim.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        Log.d(LOG_TAG, "getLastLocation failed");
                    });
        } catch (SecurityException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
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
            if (!Utils.cityExists(tmp.getCityId(), MainActivity.this)) {
                ContentValues cv = new ContentValues();
                cv.put(StormDataProvider.KEY_ID, tmp.getCityId());
                cv.put(StormDataProvider.KEY_CITYNAME, tmp.getCityName());
                getContentResolver().insert(StormDataProvider.CONTENT_URI, cv);

                finish();
            } else {
                Toast.makeText(this, R.string.message_city_exists, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.message_no_such_city, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Method for initating process of downloading data
     */
    public void refreshData() {
        Log.d("StormMonitor", "Initating data downloading...");
        recyclerView.setVisibility(View.GONE);
        loadingAnim.setVisibility(View.VISIBLE);

        getLoaderManager().initLoader(1, null, new StormWebLoader());
    }

    private class StormCursorLoader implements LoaderManager.LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(MainActivity.this,
                    StormDataProvider.CONTENT_URI, null, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            rcAdapter.swapCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            rcAdapter.swapCursor(null);
        }
    }

    private class StormWebLoader implements LoaderManager.LoaderCallbacks<Integer> {

        @Override
        public Loader<Integer> onCreateLoader(int id, Bundle args) {
            final List<StormData> cities = Utils.getAllData(MainActivity.this);
            return new AsyncTaskLoader<Integer>(MainActivity.this) {
                @Override
                protected void onStartLoading() {
                    forceLoad();
                }

                @Override
                public Integer loadInBackground() {
                    Integer result = -1;

                    if (cities != null && !cities.isEmpty()) {
                        // We list of the cities so download process can be started
                        result = Utils.getStormData(cities, MainActivity.this);
                    }

                    return result;
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<Integer> loader, Integer result) {
            switch (result) {
                case 1:
                    break;
                case 2:
                    Toast.makeText(MainActivity.this, R.string.error_no_connection, Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(MainActivity.this, R.string.error_unknown, Toast.LENGTH_LONG).show();
                    Log.d("ConnError", result.toString());
            }

            loadingAnim.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setEdgeItemsCenteringEnabled(true);

            getPreferences(0).edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply();
            MainActivity.this.getLoaderManager().destroyLoader(loader.getId());
        }

        @Override
        public void onLoaderReset(Loader<Integer> loader) {

        }
    }
}
