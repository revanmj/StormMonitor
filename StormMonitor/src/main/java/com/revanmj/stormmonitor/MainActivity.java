package com.revanmj.stormmonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.revanmj.stormmonitor.logic.CheckConnection;
import com.revanmj.stormmonitor.logic.Downloader;
import com.revanmj.stormmonitor.logic.JSONparser;
import com.revanmj.stormmonitor.model.StormData;
import com.revanmj.stormmonitor.sql.CitiesAssetHelper;
import com.revanmj.stormmonitor.sql.StormOpenHelper;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private List<StormData> cityStorm;
    private StormOpenHelper db;
    private StormDataAdapter sdAdapter;
    private MenuItem refreshButton;
    private Menu mainMenu;
    private boolean start = true;
    private ListView lista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new StormOpenHelper(this);

        cityStorm = db.getAllCities();
        sdAdapter = new StormDataAdapter(cityStorm, this);

        lista = (ListView) findViewById(R.id.listView);
        lista.setAdapter(sdAdapter);
        registerForContextMenu(lista);
    }

    @Override
    protected  void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cityStorm != null)
            RefreshData();
        start = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mainMenu = menu;
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
                String url = "http://antistorm.eu/?miasto=";
                String name = cityStorm.get(info.position).getMiasto().toLowerCase().replace(' ', '-').replace('ą','a').replace('ę','e').replace('ć','c').replace('ł','l').replace('ń','n').replace('ó','o').replace('ś','s').replace('ż','ź').replace('ź','z');
                url = url + name;
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
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
                        stormData.setMiasto("Pobieranie nie powiodło się!");
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
                postep = ProgressDialog.show(MainActivity.this, "Pobieranie", "Trwa pobieranie danych ...", true, false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                Intent search_a = new Intent(MainActivity.this, search.class);
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
        if (CheckConnection.isHttpsAvalable("http://antistorm.eu/")) {
            cityStorm = db.getAllCities();
            JSONStormTask task = new JSONStormTask();
            task.execute(cityStorm);
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
