package pl.revanmj.stormmonitor;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
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
import pl.revanmj.stormmonitor.model.DownloadResult;
import pl.revanmj.stormmonitor.model.StormData;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.winsontan520.wversionmanager.library.WVersionManager;

import io.fabric.sdk.android.Fabric;

import java.util.List;

/**
 * Created by revanmj on 14.07.2013.
 */

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    // Data for updater from WVersionManager library
    private final String updateApkUrl = "https://github.com/revanmj/StormMonitor/raw/master/StormMonitor.apk";
    private final String updateChangelogUrl = "https://github.com/revanmj/StormMonitor/raw/master/updates.json";

    // URL for opening a map in WebView
    private final String serviceUrl = "http://antistorm.eu/";

    private MainRecyclerViewAdapter rcAdapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout mySwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mySwipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swiperefresh);

        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        RefreshData(true);
                    }
                }
        );
        mySwipeRefreshLayout.setColorSchemeResources(R.color.md_blue_500);

        // Setting up FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_map);
        fab.show();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(MainActivity.this, DetailsActivity.class);
                browserIntent.putExtra("url", serviceUrl + "/m/");
                browserIntent.putExtra("title", "map");
                startActivity(browserIntent);
            }
        });

        // Setting up RecyclerView
        rcAdapter = new MainRecyclerViewAdapter(this);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        RefreshData(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
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
    private void setData(DownloadResult result)
    {
        if (result.getResultCode() == 1) {

            for (StormData city : result.getCitiesData()) {
                ContentValues cv = new ContentValues();
                cv.put(StormDataProvider.KEY_STORMCHANCE, city.getStormChance());
                cv.put(StormDataProvider.KEY_STORMTIME, city.getStormTime());
                cv.put(StormDataProvider.KEY_RAINCHANCE, city.getRainChance());
                cv.put(StormDataProvider.KEY_RAINTIME, city.getRainTime());
                String selection = StormDataProvider.KEY_ID + " = ?";
                String[] selArgs = {Integer.toString(city.getCityId())};
                getContentResolver().update(StormDataProvider.CONTENT_URI, cv, selection, selArgs);
            }

        } else {
            String error = getResources().getString(R.string.error_unknown) + result.getResultCode();

            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("Error code: " + result.getResultCode())
                    .putContentType("Error")
                    .putContentId("downloadError"));

            if (result.getResultCode() == 2)
                error = getResources().getString(R.string.error_no_connection);

            Snackbar.make(mySwipeRefreshLayout, error, Snackbar.LENGTH_LONG).show();
        }

        mySwipeRefreshLayout.setRefreshing(false);
    }

    /**
     * AsyncTask responsible for downloading data and showing ProgressDialog while doing that
     */
    private class JSONStormTask extends AsyncTask<List<StormData>, Void, DownloadResult> {

        @Override
        protected DownloadResult doInBackground(List<StormData>... params) {
            DownloadResult result = null;

            if (params[0] != null) {
                // We list of the cities so download process can be started
                result = Utils.getStormData(params[0]);
            }

            return result;
        }

        @Override
        protected void onPostExecute(DownloadResult result) {
            setData(result);
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }
    }

    /**
     * Method supporting ActionBar's menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                Intent search_a = new Intent(MainActivity.this, SearchActivity.class);
                MainActivity.this.startActivity(search_a);
                RefreshData(false);
                return true;
            case R.id.action_about:
                Intent about = new Intent(MainActivity.this, AboutActivity.class);
                MainActivity.this.startActivity(about);
                return true;
            case R.id.action_refresh:
                RefreshData(false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Method for initating process of downloading data
     */
    public void RefreshData(Boolean byGesture) {
        if (!byGesture)
            mySwipeRefreshLayout.setRefreshing(true);

        List<StormData> cities = Utils.getAllData(this);

        Log.d("StormMonitor", "Initating data downloading, city ids: " + cities);
        JSONStormTask task = new JSONStormTask();
        task.execute(cities);
    }
}
