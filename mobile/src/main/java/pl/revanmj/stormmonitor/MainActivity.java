package pl.revanmj.stormmonitor;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
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

import com.crashlytics.android.Crashlytics;
import com.winsontan520.wversionmanager.library.WVersionManager;

import pl.revanmj.stormmonitor.adapters.StormRcAdapter;
import pl.revanmj.stormmonitor.data.StormDataProvider;
import pl.revanmj.stormmonitor.logic.Utils;
import pl.revanmj.stormmonitor.logic.SwipeToDelTouchCallback;

import io.fabric.sdk.android.Fabric;

/**
 * Created by revanmj on 14.07.2013.
 */

public class MainActivity extends AppCompatActivity {
    // Data for updater from WVersionManager library
    private static final String UPDATE_APK_URL = "https://github.com/revanmj/StormMonitor/raw/master/StormMonitor.apk";
    private static final String UPDATE_CHANGELOG_URL = "https://github.com/revanmj/StormMonitor/raw/master/updates.json";
    // URL for opening a menu_map in WebView
    private static final String SERVICE_URL = "http://antistorm.eu/m/";

    private static final String KEY_LAST_UPDATE = "lastUpdate";

    private static final int LOADER_STORM_PROVIDER = 1;
    private static final int LOADER_STORM_WEBSERVICE = 2;

    private StormRcAdapter mRcAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private StormWebLoader mStormWebLoader;

    private CustomTabsClient mClient;
    private CustomTabsSession mCustomTabsSession;
    private CustomTabsServiceConnection mCustomTabsServiceConnection;
    private String chromePackageName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);

        mSwipeRefreshLayout.setOnRefreshListener(
                () -> {
                    // This method performs the actual data-refresh operation.
                    // The method calls setRefreshing(false) when it's finished.
                    refreshData(true);
                }
        );
        mSwipeRefreshLayout.setColorSchemeResources(R.color.md_blue_500);

        // Setting up RecyclerView
        mRcAdapter = new StormRcAdapter();
        recyclerView.setAdapter(mRcAdapter);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);

        SwipeToDelTouchCallback stdcallback = new SwipeToDelTouchCallback(this);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(stdcallback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mStormWebLoader = new StormWebLoader(this);
        if (savedInstanceState == null) {
            getSupportLoaderManager().initLoader(LOADER_STORM_PROVIDER, null, new StormCursorLoader(this));
            getSupportLoaderManager().initLoader(LOADER_STORM_WEBSERVICE, null, mStormWebLoader);
        }

        // Setting up updater form WVersionManager library
        WVersionManager versionManager = new WVersionManager(this);
        versionManager.setVersionContentUrl(UPDATE_CHANGELOG_URL);
        versionManager.setUpdateNowLabel(getString(R.string.dialog_update));
        versionManager.setRemindMeLaterLabel(getString(R.string.dialog_remind));
        versionManager.setIgnoreThisVersionLabel(getString(R.string.dialog_ignore));
        versionManager.setTitle(getString(R.string.label_update));
        versionManager.setUpdateUrl(UPDATE_APK_URL);
        versionManager.setReminderTimer(1440); // this mean checkVersion() will not take effect within 10 minutes
        versionManager.checkVersion();

        // Check if chrome is available
        chromePackageName = Utils.getChromeChannel(this);

        // Setting up FAB
        FloatingActionButton fab = findViewById(R.id.fab_map);
        fab.show();
        fab.setOnClickListener(view -> {
            String chromePackageName = Utils.getChromeChannel(this);
            if (chromePackageName != null) {
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(getResources().getColor(R.color.md_blue_500));
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(this, Uri.parse(SERVICE_URL));
            } else {
                Intent webviewIntent = new Intent(MainActivity.this, WebViewActivity.class);
                webviewIntent.putExtra("url", SERVICE_URL);
                webviewIntent.putExtra("title", "map");
                startActivity(webviewIntent);
            }
        });
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
                        mCustomTabsSession.mayLaunchUrl(Uri.parse(SERVICE_URL + "m/"), null, null);
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

    /**
     * Method for initating process of downloading data
     */
    public void refreshData(Boolean byGesture) {
        if (!byGesture)
            mSwipeRefreshLayout.setRefreshing(true);

        getSupportLoaderManager().restartLoader(LOADER_STORM_WEBSERVICE, null, mStormWebLoader);
    }

    /**
     * Method for updating database after downloading data and refreshing the ListVieww (if succesful)
     * or showing anToast with an error if downloading failed.
     * @param result Integer value representing operation status (failed or success)
     */
    private void onDownloadFinished(Integer result) {
        mSwipeRefreshLayout.setRefreshing(false);

        switch (result) {
            case Utils.SUCCESS:
                // Download successful
                getPreferences(0).edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply();
                break;
            case Utils.CONNECTION_ERROR:
                Snackbar.make(mSwipeRefreshLayout, R.string.error_no_connection, Snackbar.LENGTH_LONG);
                break;
            default:
                Snackbar.make(mSwipeRefreshLayout, R.string.error_unknown, Snackbar.LENGTH_LONG);
                Log.e("ConnError", result.toString());
        }
    }

    private class StormCursorLoader implements LoaderManager.LoaderCallbacks<Cursor> {
        private Context context;

        StormCursorLoader(Context context) {
            this.context = context;
        }
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(context, StormDataProvider.CONTENT_URI,
                    null, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mRcAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mRcAdapter.swapCursor(null);
        }
    }

    private class StormWebLoader implements LoaderManager.LoaderCallbacks<Integer> {
        private Context context;

        StormWebLoader(Context context) {
            this.context = context;
        }
        @Override
        public Loader<Integer> onCreateLoader(int id, Bundle args) {
            return new AsyncTaskLoader<Integer>(context) {
                @Override
                public Integer loadInBackground() {
                    return Utils.downloadStormData(Utils.loadCitiesFromDb(context), context);
                }

                @Override
                protected void onStartLoading() {
                    forceLoad();
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<Integer> loader, Integer data) {
            onDownloadFinished(data);
        }

        @Override
        public void onLoaderReset(Loader<Integer> loader) {}
    }
}
