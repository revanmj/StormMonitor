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

    private void addLocationCity(String data) {
        CitiesAssetHelper cities_db = new CitiesAssetHelper(this);
        StormData tmp = cities_db.getCity(data);
        boolean city_exists = false;
        if (tmp != null) {
            for (int i = 0; i < cityStorm.size(); i++)
                if (cityStorm.get(i).getMiasto_id() == tmp.getMiasto_id())
                    city_exists = true;
            if (!city_exists) {
                    db.addCity(tmp);
                    RefreshData();
                    Log.i("revanmj.Storm", "Added: " + cityStorm.get(cityStorm.size()-1));
            } else {
                Toast.makeText(this, R.string.message_city_exists, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.message_no_such_city, Toast.LENGTH_SHORT).show();
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

    public class CityAsyncTask extends AsyncTask<String, String, String> {
        Activity act;
        double latitude;
        double longitude;
        protected ProgressDialog postep;

        public CityAsyncTask(Activity act) {
            this.act = act;
        }

        @Override
        protected String doInBackground(String... params) {
            String result = "";
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, false);
            Location location = locationManager.getLastKnownLocation(provider);
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            Geocoder geocoder = new Geocoder(act, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude,
                        longitude, 1);
                Log.e("Addresses", "-->" + addresses);
                Address tmp = addresses.get(0);
                result = tmp.getAddressLine(1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            postep.dismiss();
            addLocationCity(result);
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            postep = ProgressDialog.show(MainActivity.this, "Lokalizowanie", "Trwa ustalanie lokalizacji ...", true, false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                AlertDialog.Builder builder_d = new AlertDialog.Builder(MainActivity.this);
                LayoutInflater infl = (LayoutInflater) getApplication().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                final View widok = infl.inflate(R.layout.add_city, null);
                builder_d.setView(widok);
                builder_d.setCancelable(false);
                builder_d.setTitle(R.string.menu_addcity);
                builder_d.setPositiveButton(R.string.button_add, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
                builder_d.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        imm.toggleSoftInput(InputMethodManager.RESULT_UNCHANGED_HIDDEN, 0);
                        dialog.cancel();
                    }
                });
                final AlertDialog dodawanie = builder_d.create();
                dodawanie.show();
                dodawanie.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if (widok != null) {
                            EditText pole = (EditText) widok.findViewById(R.id.cityIDfield);
                            Integer liczba = Integer.parseInt(pole.getText().toString());
                            boolean blad = false;
                            if (cityStorm != null)
                                for (int i = 0; i < cityStorm.size(); i++)
                                    if (cityStorm.get(i).getMiasto_id() == liczba)
                                        blad = true;
                            if (!blad) {
                                if (liczba.intValue() < 439) {
                                    StormData tmp = new StormData();
                                    tmp.setMiasto_id(liczba);
                                    db.addCity(tmp);
                                    RefreshData();
                                    Log.i("revanmj.Storm", "Added: " + cityStorm.get(cityStorm.size()-1));
                                    imm.toggleSoftInput(InputMethodManager.RESULT_UNCHANGED_HIDDEN, 0);
                                    dodawanie.dismiss();
                                }
                                else
                                    Toast.makeText(v.getContext(), R.string.message_no_such_city, Toast.LENGTH_SHORT).show();
                            }
                            else
                                Toast.makeText(v.getContext(), R.string.message_city_exists, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
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
            case R.id.action_add_gps:
                CityAsyncTask t = new CityAsyncTask(this);
                t.execute();
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
