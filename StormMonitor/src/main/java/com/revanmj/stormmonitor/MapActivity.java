package com.revanmj.stormmonitor;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

public class MapActivity extends Activity {
    Bitmap radar, probability, visual, velocity, estofex;
    Canvas image;
    boolean error;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        error = false;
        RefreshMap();
        if (error) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
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
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_map, container, false);
            return rootView;
        }
    }

    public Bitmap getBitmapFromURL(String src) {
        try {
            java.net.URL url = new java.net.URL(src);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void RefreshMap() {
        String[] adresy = new String[5];
        adresy[0] = "http://antistorm.eu/radar/radar.png";
        adresy[4] = "http://antistorm.eu/currentImgs/estofex.png";
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR), month = c.get(Calendar.MONTH), day = c.get(Calendar.DAY_OF_MONTH), hour = c.get(Calendar.HOUR), minutes = c.get(Calendar.MINUTE);
        if (minutes != 0 && minutes != 15 && minutes != 30 && minutes != 45) {
            if (minutes > 0 && minutes < 15)
                minutes = 0;
            else if (minutes > 15 && minutes < 30)
                minutes = 15;
            else if (minutes > 30 && minutes < 45)
                minutes = 30;
            else if (minutes > 45 && minutes < 59)
                minutes = 45;
        }
        adresy[1] = "http://antistorm.eu/archive/" + year + "." + month + "." + day +"/" + hour + "-" + minutes + "-probabilitiesImg.png";
        adresy[2] = "http://antistorm.eu/archive/" + year + "." + month + "." + day +"/" + hour + "-" + minutes + "-velocityMapImg.png";
        adresy[2] = "http://antistorm.eu/archive/" + year + "." + month + "." + day +"/" + hour + "-" + minutes + "-stormVisualImg.png";
        BitmapTask task = new BitmapTask();
        task.execute(adresy);

        if (radar != null && probability != null && visual != null && velocity != null && estofex != null) {
            Bitmap mapa = BitmapFactory.decodeResource(getResources(), R.drawable.map);
            image = new Canvas(mapa);
            image.drawBitmap(radar, 0f, 0f, null);
            image.drawBitmap(probability, 0f, 0f, null);
            image.drawBitmap(visual, 0f, 0f, null);
            image.drawBitmap(velocity, 0f, 0f, null);
            image.drawBitmap(estofex, 0f, 0f, null);
            SurfaceView mapView = (SurfaceView) findViewById(R.id.surfaceView);
            mapView.draw(image);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
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

    private class BitmapTask extends AsyncTask<String, Void, ArrayList<Bitmap>> {

        protected ProgressDialog postep;
        ArrayList<Bitmap> lista;

        @Override
        protected ArrayList<Bitmap> doInBackground(String... params) {
            lista = new ArrayList<Bitmap>();
            if (params[0] != null) {
                int rozmiar = params.length;
                Bitmap tmp;
                for (int i = 0; i < rozmiar; i++) {
                    tmp = getBitmapFromURL(params[i]);
                    if (tmp == null && i == 2)
                       tmp = BitmapFactory.decodeResource(getResources(), R.drawable.blankVelocity);
                    else if (tmp == null)
                       tmp = BitmapFactory.decodeResource(getResources(), R.drawable.blank);
                    lista.add(tmp);
                }
                return lista;
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Bitmap> result) {
            if (result != null) {
                radar = result.get(0);
                probability = result.get(1);
                velocity = result.get(2);
                visual = result.get(3);
                estofex = result.get(4);
            } else
                error = true;
            if (postep != null)
                postep.dismiss();
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            postep = ProgressDialog.show(MapActivity.this, "Pobieranie", "Trwa pobieranie danych ...", true, false);
        }
    }

}
