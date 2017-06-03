package pl.revanmj.stormmonitor;

import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.LoaderManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.CurvedChildLayoutManager;
import android.support.wearable.view.WearableRecyclerView;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import pl.revanmj.stormmonitor.data.CitiesAssetHelper;
import pl.revanmj.stormmonitor.data.StormData;
import pl.revanmj.stormmonitor.data.StormDataProvider;

public class MainActivity extends WearableActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, WearableActionDrawer.OnMenuItemClickListener {

    private static final String PERMISSION_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
    private static final String KEY_LAST_UPDATE = "lastUpdate";

    private WearableRecyclerView recyclerView;
    private MainRecyclerViewAdapter rcAdapter;
    private WearableDrawerLayout mContainerView;
    private WearableActionDrawer actionDrawer;
    private ProgressBar loadingAnim;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContainerView = (WearableDrawerLayout) findViewById(R.id.container);
        recyclerView = (WearableRecyclerView) findViewById(R.id.recycler_launcher_view);
        loadingAnim = (ProgressBar) findViewById(R.id.progressBar);

        rcAdapter = new MainRecyclerViewAdapter(this);
        recyclerView.setAdapter(rcAdapter);
        recyclerView.setCenterEdgeItems(true);
        CurvedChildLayoutManager mChildLayoutManager = new CurvedChildLayoutManager(this);
        recyclerView.setLayoutManager(mChildLayoutManager);

        actionDrawer = (WearableActionDrawer) findViewById(R.id.bottom_action_drawer);
        actionDrawer.setOnMenuItemClickListener(this);
        mContainerView.peekDrawer(Gravity.BOTTOM);

        getLoaderManager().initLoader(1, null, this);

        loadingAnim.setProgress(0);

        if (System.currentTimeMillis() - getPreferences(0).getLong(KEY_LAST_UPDATE, 0) > 900000) {
            refreshData();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, StormDataProvider.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor cursor) {
        rcAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(android.content.Loader<Cursor> loader) {
        rcAdapter.swapCursor(null);
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
        mContainerView.closeDrawer(actionDrawer);
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
            loadingAnim.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);

            GoogleApiConnectionCallbacks connectionCallbacks = new GoogleApiConnectionCallbacks();
            googleApiClient = new GoogleApiClient.Builder(MainActivity.this)
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
     * Method for updating database after downloading data and refreshing the ListVieww (if succesful)
     * or showing AlertDialog with an error if downloading failed.
     * @param result
     */
    private void downloadFinished(Integer result)
    {
        switch (result) {
            case 1:
                break;
            case 2:
                Toast.makeText(this, R.string.error_no_connection, Toast.LENGTH_LONG);
                break;
            default:
                Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_LONG);
                Log.d("ConnError", result.toString());
        }

        loadingAnim.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setCenterEdgeItems(true);

        getPreferences(0).edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply();
    }

    /**
     * Method for initating process of downloading data
     */
    public void refreshData() {
        List<StormData> cities = Utils.getAllData(this);

        Log.d("StormMonitor", "Initating data downloading, city ids: " + cities);
        JSONStormTask task = new JSONStormTask();
        task.execute(cities);
    }

    /**
     * AsyncTask responsible for downloading data and showing ProgressDialog while doing that
     */
    private class JSONStormTask extends AsyncTask<List<StormData>, Void, Integer> {

        @Override
        protected Integer doInBackground(List<StormData>... params) {
            int result = -1;

            if (params[0] != null && !params[0].isEmpty()) {
                // We list of the cities so download process can be started
                result = Utils.getStormData(params[0], MainActivity.this);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            downloadFinished(result);
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute(){
            recyclerView.setVisibility(View.GONE);
            loadingAnim.setVisibility(View.VISIBLE);

            super.onPreExecute();
        }
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
                Log.d("MainActivity", "location is null!");
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
            loadingAnim.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setCenterEdgeItems(true);
            googleApiClient.disconnect();
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }
    }
}
