package pl.revanmj.stormmonitor;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.crashlytics.android.Crashlytics;

import pl.revanmj.stormmonitor.adapters.MainViewAdapter;
import pl.revanmj.stormmonitor.logic.Downloader;
import pl.revanmj.stormmonitor.model.DownloadResult;
import pl.revanmj.stormmonitor.model.StormData;
import pl.revanmj.stormmonitor.sql.StormOpenHelper;
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
    private MenuItem refreshButton;
    private boolean start = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        // Getting all cities from the database
        StormOpenHelper db = new StormOpenHelper(this);
        cityStorm = db.getAllCities();
        db.close();

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
        RefreshData();
        start = false;
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

        if (refreshButton != null && refreshButton.getActionView() != null) {
            refreshButton.getActionView().clearAnimation();
            refreshButton.setActionView(null);
        }
    }

    /**
     * AsyncTask responsible for downloading data and showing ProgressDialog while doing that
     */
    private class JSONStormTask extends AsyncTask<List<StormData>, Void, DownloadResult> {

        protected ProgressDialog postep;

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
            if (postep != null)
                postep.dismiss();
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            if (start)
                postep = ProgressDialog.show(MainActivity.this, null, getResources().getString(R.string.label_downloading), true, false);
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
                RefreshData();
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
                refreshButton = item;
                LayoutInflater inflater = (LayoutInflater) getApplication().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_icon, null);
                Animation rotation = AnimationUtils.loadAnimation(getApplication(), R.anim.refresh);
                rotation.setRepeatCount(Animation.INFINITE);
                iv.startAnimation(rotation);
                refreshButton.setActionView(iv);
                RefreshData();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Method for initating process of downloading data
     */
    public void RefreshData() {
        StormOpenHelper db = new StormOpenHelper(this);
        cityStorm = db.getAllCities();
        db.close();

        JSONStormTask task = new JSONStormTask();
        task.execute(cityStorm);
    }
}
