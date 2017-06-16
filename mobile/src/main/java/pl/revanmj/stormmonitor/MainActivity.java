package pl.revanmj.stormmonitor;

import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.crashlytics.android.Crashlytics;

import pl.revanmj.stormmonitor.adapters.MainRecyclerViewAdapter;
import pl.revanmj.stormmonitor.data.StormDataProvider;
import pl.revanmj.stormmonitor.logic.Utils;
import pl.revanmj.stormmonitor.logic.SwipeToDelTouchCallback;
import pl.revanmj.stormmonitor.model.StormData;

import com.winsontan520.wversionmanager.library.WVersionManager;

import io.fabric.sdk.android.Fabric;

import java.util.List;

/**
 * Created by revanmj on 14.07.2013.
 */

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    // Data for updater from WVersionManager library
    private static final String updateApkUrl = "https://github.com/revanmj/StormMonitor/raw/master/StormMonitor.apk";
    private static final String updateChangelogUrl = "https://github.com/revanmj/StormMonitor/raw/master/updates.json";

    // URL for opening a menu_map in WebView
    private static final String serviceUrl = "http://antistorm.eu/";

    private static final String KEY_LAST_UPDATE = "lastUpdate";

    private MainRecyclerViewAdapter rcAdapter;
    private SwipeRefreshLayout mySwipeRefreshLayout;

    private CustomTabsClient mClient;
    private CustomTabsSession mCustomTabsSession;
    private CustomTabsServiceConnection mCustomTabsServiceConnection;
    private String chromePackageName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mySwipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swiperefresh);

        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        refreshData(true);
                    }
                }
        );
        mySwipeRefreshLayout.setColorSchemeResources(R.color.md_blue_500);

        // Setting up RecyclerView
        rcAdapter = new MainRecyclerViewAdapter();
        recyclerView.setAdapter(rcAdapter);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);

        SwipeToDelTouchCallback stdcallback = new SwipeToDelTouchCallback(this);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(stdcallback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        getSupportLoaderManager().initLoader(1, null, this);

        // Setting up updater form WVersionManager library
        WVersionManager versionManager = new WVersionManager(this);
        versionManager.setVersionContentUrl(updateChangelogUrl);
        versionManager.setUpdateNowLabel(getString(R.string.dialog_update));
        versionManager.setRemindMeLaterLabel(getString(R.string.dialog_remind));
        versionManager.setIgnoreThisVersionLabel(getString(R.string.dialog_ignore));
        versionManager.setTitle(getString(R.string.label_update));
        versionManager.setUpdateUrl(updateApkUrl);
        versionManager.setReminderTimer(1440); // this mean checkVersion() will not take effect within 10 minutes
        versionManager.checkVersion();

        // Check if chrome is available
        chromePackageName = Utils.chromeChannel(this);

        // Setting up FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_map);
        fab.show();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (chromePackageName != null) {
                    CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder(mCustomTabsSession)
                            .setToolbarColor(ContextCompat.getColor(MainActivity.this, R.color.md_blue_500))
                            .setCloseButtonIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_arrow_back))
                            .setShowTitle(true)
                            .build();
                    customTabsIntent.launchUrl(MainActivity.this, Uri.parse(serviceUrl + "m/"));
                } else {
                    Intent browserIntent = new Intent(MainActivity.this, WebViewActivity.class);
                    browserIntent.putExtra("url", serviceUrl + "m/");
                    browserIntent.putExtra("title", "menu_map");
                    startActivity(browserIntent);
                }
            }
        });

        // If more there was more than 15 minutes since last update, refresh data
        if (System.currentTimeMillis() - getPreferences(0).getLong(KEY_LAST_UPDATE, 0) > 900000) {
            refreshData(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Warmup Chrome Custom Tabs client
        if (chromePackageName != null) {
            mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
                @Override
                public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                    //Pre-warming
                    mClient = customTabsClient;
                    if (mClient != null) {
                        mClient.warmup(0L);
                        mCustomTabsSession = mClient.newSession(null);
                        mCustomTabsSession.mayLaunchUrl(Uri.parse(serviceUrl + "m/"), null, null);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mClient = null;
                    mCustomTabsSession = null;
                    mCustomTabsServiceConnection = null;
                }
            };
            CustomTabsClient.bindCustomTabsService(MainActivity.this, chromePackageName, mCustomTabsServiceConnection);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        this.unbindService(mCustomTabsServiceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                Intent search_a = new Intent(MainActivity.this, SearchActivity.class);
                MainActivity.this.startActivity(search_a);
                refreshData(false);
                return true;
            case R.id.action_refresh:
                refreshData(false);
                return true;
            case R.id.action_settings:
                Intent preferencesIntent = new Intent(MainActivity.this, PreferencesActivity.class);
                startActivity(preferencesIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, StormDataProvider.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        rcAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        rcAdapter.swapCursor(null);
    }

    /**
     * Method for updating database after downloading data and refreshing the ListVieww (if succesful)
     * or showing AlertDialog with an error if downloading failed.
     * @param result
     */
    private void downloadFinished(Integer result)
    {
        mySwipeRefreshLayout.setRefreshing(false);

        switch (result) {
            case 1:
                break;
            case 2:
                Snackbar.make(mySwipeRefreshLayout, R.string.error_no_connection, Snackbar.LENGTH_LONG);
                break;
            default:
                Snackbar.make(mySwipeRefreshLayout, R.string.error_unknown, Snackbar.LENGTH_LONG);
                Log.d("ConnError", result.toString());
        }

        getPreferences(0).edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply();
    }

    /**
     * AsyncTask responsible for downloading data and showing ProgressDialog while doing that
     */
    private class JSONStormTask extends AsyncTask<List<StormData>, Void, Integer> {

        @Override
        protected Integer doInBackground(List<StormData>... params) {
            int result = -1;

            if (params[0] != null) {
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
            super.onPreExecute();
        }
    }

    /**
     * Method for initating process of downloading data
     */
    public void refreshData(Boolean byGesture) {
        if (!byGesture)
            mySwipeRefreshLayout.setRefreshing(true);

        List<StormData> cities = Utils.getAllData(this);

        Log.d("StormMonitor", "Initating data downloading, city ids: " + cities);
        JSONStormTask task = new JSONStormTask();
        task.execute(cities);
    }
}
