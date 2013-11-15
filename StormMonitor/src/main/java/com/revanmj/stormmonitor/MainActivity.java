package com.revanmj.stormmonitor;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    private List<StormData> cityStorm;
    private List<Integer> cities;
    private StormDataAdapter sdAdapter;
    private MenuItem refreshButton;
    private Menu mainMenu;
    private boolean start = true;
    private boolean err_down = false;
    private ListView lista;
    private File miasta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cityStorm = new ArrayList<StormData>();
        sdAdapter = new StormDataAdapter(cityStorm, this);
        miasta = new File(getApplicationContext().getFilesDir(), "cities.dat");
        cities = new ArrayList<Integer>();

        try {
            cities.addAll(DataFile.ReadFromFile(miasta));
        } catch (FileNotFoundException e) {}
        catch (IOException e) {}

        //InitializeCities();

        RefreshData();

        //mainMenu.performIdentifierAction(mainMenu.getItem(0).getItemId(),0);

        lista = (ListView) findViewById(R.id.listView);
        lista.setAdapter(sdAdapter);
        start = false;
        registerForContextMenu(lista);
    }

    @Override
    protected  void onDestroy() {
        try {
            miasta.delete();
            DataFile.WriteToFile(cities, miasta);
        } catch (FileNotFoundException e) {}
        catch (IOException e) {};
        super.onDestroy();
    }

    @Override
    protected  void onPause() {
        try {
            miasta.delete();
            DataFile.WriteToFile(cities, miasta);
        } catch (FileNotFoundException e) {}
        catch (IOException e) {};
        super.onPause();
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

        menu.setHeaderTitle(R.string.menu_delete_q);

        menu.add(R.string.menu_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Here's how you can get the correct item in onContextItemSelected()
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        //Object listAdapter = lista.getAdapter().getItem(info.position);
        cities.remove(info.position);
        cityStorm.remove(info.position);
        sdAdapter.notifyDataSetChanged();

        return true;
    }

    private void setData(List<StormData> result)
    {
        cityStorm.clear();
        cityStorm.addAll(result);
        sdAdapter.notifyDataSetChanged();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (refreshButton != null && refreshButton.getActionView() != null) {
              refreshButton.getActionView().clearAnimation();
              refreshButton.setActionView(null);
            }
        }
    }

    private class JSONStormTask extends AsyncTask<List<Integer>, Void, List<StormData>> {

        protected ProgressDialog postep;

        @Override
        protected List<StormData> doInBackground(List<Integer>... params) {
            List<StormData> lista = new ArrayList<StormData>();

            if (params[0] != null) {
                int ile = params[0].size();

                for (int i = 0; i < ile; i++) {
                    StormData stormData = new StormData();
                    String data = ( (new Downloader()).getStormData(params[0].get(i)));

                    if (data != null) {
                        try {
                            stormData = JSONparser.getStormData(data);
                            lista.add(stormData);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        err_down = true;
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
        // Handle item selection
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
                            if (cities != null)
                                for (int i = 0; i < cities.size(); i++)
                                    if (cities.get(i).equals(liczba))
                                        blad = true;
                            if (!blad) {
                                if (liczba.intValue() < 439) {
                                    cities.add(liczba);
                                    RefreshData();
                                    sdAdapter.notifyDataSetChanged();
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
            //    case R.id.action_test:
            //    Intent test = new Intent(MainActivity.this, testData.class);
            //    MainActivity.this.startActivity(test);
            //    return true;
            case R.id.action_about:
                Intent about = new Intent(MainActivity.this, About.class);
                MainActivity.this.startActivity(about);
                return true;
            case R.id.action_refresh:
                refreshButton = item;
                LayoutInflater inflater = (LayoutInflater) getApplication().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_icon, null);
                Animation rotation = AnimationUtils.loadAnimation(getApplication(), R.anim.refresh);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    rotation.setRepeatCount(Animation.INFINITE);
                    iv.startAnimation(rotation);
                    refreshButton.setActionView(iv);
                }

                RefreshData();
                sdAdapter.notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void RefreshData() {
        JSONStormTask task = new JSONStormTask();
        task.execute(cities);
        if (err_down) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                if (refreshButton != null && refreshButton.getActionView() != null) {
                    refreshButton.getActionView().clearAnimation();
                    refreshButton.setActionView(null);
                }
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
