package pl.revanmj.stormmonitor;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.crashlytics.android.Crashlytics;

import pl.revanmj.stormmonitor.adapters.MainViewAdapter;
import pl.revanmj.stormmonitor.logic.Downloader;
import pl.revanmj.stormmonitor.model.DownloadResult;
import pl.revanmj.stormmonitor.model.StormData;
import pl.revanmj.stormmonitor.sql.StormOpenHelper;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.winsontan520.wversionmanager.library.WVersionManager;

import io.fabric.sdk.android.Fabric;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    // Data for updater from WVersionManager library
    private final String updateApkUrl = "https://github.com/revanmj/StormMonitor/raw/master/StormMonitor.apk";
    private final String updateChangelogUrl = "https://github.com/revanmj/StormMonitor/raw/master/updates.json";

    // URL for opening a map in WebView
    private final String serviceUrl = "http://antistorm.eu/";

    private List<StormData> cityStorm;
    private MainViewAdapter sdAdapter;
    private SwipeRefreshLayout mySwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        // Getting all cities from the database
        StormOpenHelper db = new StormOpenHelper(this);
        cityStorm = db.getAllCities();
        db.close();

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

        // Setting up the ListView
        sdAdapter = new MainViewAdapter(cityStorm, this);
        ListView lista = (ListView) findViewById(R.id.listView);
        lista.setAdapter(sdAdapter);
        registerForContextMenu(lista);

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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId() == R.id.listView) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.context, menu);
        }
    }

    /**
     * Method for updating database after downloading data and refreshing the ListVieww (if succesful)
     * or showing AlertDialog with an error if downloading failed.
     * @param result
     */
    private void setData(DownloadResult result)
    {
        if (result.getResultCode() == 1) {
            StormOpenHelper db = new StormOpenHelper(this);

            for (StormData city : result.getCitiesData()) {
                db.updateCity(city);
            }

            cityStorm = db.getAllCities();
            db.close();

            sdAdapter.clear();
            sdAdapter.addAll(cityStorm);
            sdAdapter.notifyDataSetChanged();
        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
            alertDialogBuilder.setTitle(R.string.message_error);

            String error = getResources().getString(R.string.error_unknown) + result.getResultCode();

            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("Error code: " + result.getResultCode())
                    .putContentType("Error")
                    .putContentId("downloadError"));

            if (result.getResultCode() == 2)
                error = getResources().getString(R.string.error_no_connection);

            alertDialogBuilder
                    .setMessage(error)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    });

            alertDialogBuilder.create().show();
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
                result = Downloader.getStormData(params[0]);
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
     * Method supporting context menu of the ListView
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
            case R.id.context_delete:
                StormOpenHelper db = new StormOpenHelper(this);
                db.deleteCity(cityStorm.get(info.position));
                cityStorm = db.getAllCities();
                db.close();

                sdAdapter.clear();
                sdAdapter.addAll(cityStorm);
                sdAdapter.notifyDataSetChanged();

                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("MainView")
                        .putContentType("Action")
                        .putContentId("deletedCity"));

                return true;
        }
        return  true;
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
            case R.id.action_map:
                Intent browserIntent = new Intent(MainActivity.this, DetailsActivity.class);
                browserIntent.putExtra("url", serviceUrl + "/m/");
                browserIntent.putExtra("title", "map");
                startActivity(browserIntent);
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

        StormOpenHelper db = new StormOpenHelper(this);
        cityStorm = db.getAllCities();
        db.close();

        JSONStormTask task = new JSONStormTask();
        task.execute(cityStorm);
    }
}
