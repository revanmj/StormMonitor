package com.revanmj.stormmonitor;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
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
import com.revanmj.stormmonitor.logic.CheckConnection;
import com.revanmj.stormmonitor.logic.Downloader;
import com.revanmj.stormmonitor.logic.JSONparser;
import com.revanmj.stormmonitor.model.StormData;
import com.revanmj.stormmonitor.sql.StormOpenHelper;
import com.winsontan520.wversionmanager.library.WVersionManager;

import io.fabric.sdk.android.Fabric;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    private final String updateApkUrl = "";
    private final String updateChangelogUrl = "";
    private final String serviceUrl = "http://antistorm.eu/";
    private final String cityDataUrl = "http://antistorm.eu/?miasto=";
    private List<StormData> cityStorm;
    private StormOpenHelper db;
    private MainViewAdapter sdAdapter;
    private MenuItem refreshButton;
    private boolean start = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        setContentView(R.layout.activity_main);

        db = new StormOpenHelper(this);

        cityStorm = db.getAllCities();
        sdAdapter = new MainViewAdapter(cityStorm, this);

        ListView lista = (ListView) findViewById(R.id.listView);
        lista.setAdapter(sdAdapter);
        registerForContextMenu(lista);

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
    protected  void onDestroy() {
        super.onDestroy();
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

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
            case R.id.context_delete:
                db.deleteCity(cityStorm.get(info.position));
                cityStorm = db.getAllCities();
                sdAdapter.clear();
                sdAdapter.addAll(cityStorm);
                sdAdapter.notifyDataSetChanged();
                return true;
            case R.id.context_details:
                String name = cityStorm.get(info.position).getMiasto().toLowerCase().replace(' ', '-').replace('ą','a').replace('ę','e').replace('ć','c').replace('ł','l').replace('ń','n').replace('ó','o').replace('ś','s').replace('ż','ź').replace('ź','z');
                Intent browserIntent = new Intent(MainActivity.this, DetailsActivity.class);
                browserIntent.putExtra("url", cityDataUrl + name);
                startActivity(browserIntent);
                return true;
        }
        return  true;
    }

    private void setData(List<StormData> result)
    {
        for (int i = 0; i < result.size(); i++) {
            db.updateCity(result.get(i));
        }

        cityStorm = db.getAllCities();
        sdAdapter.clear();
        sdAdapter.addAll(cityStorm);
        sdAdapter.notifyDataSetChanged();

        if (refreshButton != null && refreshButton.getActionView() != null) {
            refreshButton.getActionView().clearAnimation();
            refreshButton.setActionView(null);
        }
    }

    private class JSONStormTask extends AsyncTask<List<StormData>, Void, List<StormData>> {

        protected ProgressDialog postep;

        @Override
        protected List<StormData> doInBackground(List<StormData>... params) {
            List<StormData> lista = new ArrayList<StormData>();

            if (params[0] != null) {
                int ile = params[0].size();

                for (int i = 0; i < ile; i++) {
                    StormData stormData = new StormData();
                    String data = ( (new Downloader()).getStormData(params[0].get(i).getMiasto_id()));

                    if (data != null) {
                        try {
                            stormData = JSONparser.getStormData(data);
                            stormData.setMiasto_id(params[0].get(i).getMiasto_id());
                            lista.add(stormData);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        stormData.setP_burzy(0);
                        stormData.setT_burzy(500);
                        stormData.setMiasto(getResources().getString(R.string.message_download_error));
                        stormData.setError(true);
                    }
                }
            }

            return lista;
        }

        @Override
        protected void onPostExecute(List<StormData> result) {
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                Intent search_a = new Intent(MainActivity.this, SearchActivity.class);
                MainActivity.this.startActivity(search_a);
                RefreshData();
                return true;
            case R.id.action_about:
                Intent about = new Intent(MainActivity.this, About.class);
                MainActivity.this.startActivity(about);
                return true;
            case R.id.action_map:
                Intent map = new Intent(MainActivity.this, MapActivity.class);
                MainActivity.this.startActivity(map);
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

    public void RefreshData() {
        if (CheckConnection.isHttpsAvalable(serviceUrl)) {
            cityStorm = db.getAllCities();
            JSONStormTask task = new JSONStormTask();
            task.execute(cityStorm);
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR), month = c.get(Calendar.MONTH) + 1, day = c.get(Calendar.DAY_OF_MONTH), hour = c.get(Calendar.HOUR_OF_DAY), minutes = c.get(Calendar.MINUTE);
            String last_d = day + "." + month + "." + year;
        } else {
            if (refreshButton != null && refreshButton.getActionView() != null) {
                refreshButton.getActionView().clearAnimation();
                refreshButton.setActionView(null);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(R.string.message_no_connection);
            builder.setTitle(R.string.message_error);
            builder.setNeutralButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            AlertDialog komunikat = builder.create();
            komunikat.show();
        }
    }
}
