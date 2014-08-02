package com.revanmj.stormmonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

public class MapActivity extends Activity {
    Bitmap radar, probability, visual, velocity, velocity_blank, estofex, blank;
    Canvas image;
    ImageViewTouch mapView;
    ProgressDialog postep;
    boolean error;
    int map_mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mapView = (ImageViewTouch) findViewById(R.id.mapView);
        blank = BitmapFactory.decodeResource(getResources(), R.drawable.blank);
        velocity_blank = BitmapFactory.decodeResource(getResources(), R.drawable.blank_velocity);
        error = false;

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mapView.getLayoutParams().height = size.x;
        mapView.getLayoutParams().width = size.x;
        mapView.bringToFront();

        SharedPreferences settings = getPreferences(0);
        map_mode = settings.getInt("map_mode", 0);
        switch (map_mode) {
            case 0:
                setTitle(R.string.title_activity_map);
                break;
            case 1:
                setTitle(R.string.title_map_rain);
                break;
        }

        RefreshMap();

        if (error) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
            builder.setMessage(R.string.message_map_error);
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

    @Override
    public void onPause()
    {
        super.onPause();
        if(postep != null)
            postep.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);

        SharedPreferences settings = getPreferences(0);
        map_mode = settings.getInt("map_mode", 0);
        MenuItem rain = menu.findItem(R.id.action_map_rain);
        MenuItem storm = menu.findItem(R.id.action_map_storm);

        if (map_mode == 0) {
            rain.setVisible(true);
            storm.setVisible(false);
        }
        else if (map_mode == 1) {
            rain.setVisible(false);
            storm.setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_map_refresh:
                RefreshMap();
                return true;
            case R.id.action_map_rain:
                switchMap();
                return true;
            case R.id.action_map_storm:
                switchMap();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void RefreshMap() {
        BitmapTask task = new BitmapTask();
        task.execute(map_mode);
    }

    public void switchMap(){
        SharedPreferences settings = getPreferences(0);
        SharedPreferences.Editor editor = settings.edit();

        if (map_mode == 0) {
            map_mode = 1;
            setTitle(R.string.title_map_rain);
        }
        else if (map_mode == 1) {
            map_mode = 0;
            setTitle(R.string.title_activity_map);
        }

        editor.putInt("map_mode", map_mode);
        editor.apply();

        invalidateOptionsMenu();

        RefreshMap();
    }

    public Bitmap getBitmapFromURL(String src) {
        try {
            java.net.URL url = new java.net.URL(src);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getHTML(int code) {
        try {
            HttpClient httpclient = new DefaultHttpClient(); // Create HTTP Client
            HttpGet httpget;
            switch (code) {
                case 0:
                    httpget = new HttpGet("http://antistorm.eu/?strona=burze");
                    break;
                case 1:
                    httpget = new HttpGet("http://antistorm.eu/?strona=radary");
                    break;
                default:
                    httpget = new HttpGet("http://antistorm.eu/?strona=burze");
            }
            HttpResponse response = httpclient.execute(httpget); // Executeit
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent(); // Create an InputStream with the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null) // Read line by line
                sb.append(line + "\n");

            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<String> getFilesURLs(int code){
        String websiteHTML = getHTML(code);

        Pattern patternProbabilities = Pattern.compile("<img.*src=[\\\"'](.*probabilitiesImg[\\.]png)[\\\"'].*>", Pattern.CASE_INSENSITIVE);
        Pattern patternVelocity = Pattern.compile("<img.*src=[\\\"'](.*velocityMapImg[\\.]png)[\\\"'].*>", Pattern.CASE_INSENSITIVE);
        Pattern patternRadar = Pattern.compile("<img.*src=[\\\"'](.*visualPhenomenon[\\.]png)[\\\"'].*>", Pattern.CASE_INSENSITIVE);

        ArrayList<String> tmp = new ArrayList<String>();

        Matcher m = patternProbabilities.matcher(websiteHTML);
        if (m.find())
            tmp.add("http://antistorm.eu/" + m.group(1));
        else
            tmp.add(null);

        m = patternVelocity.matcher(websiteHTML);
        if (m.find())
            tmp.add("http://antistorm.eu/" + m.group(1));
        else
            tmp.add(null);

        m = patternRadar.matcher(websiteHTML);
        if (m.find())
            tmp.add("http://antistorm.eu" + m.group(1));
        else
            tmp.add(null);

        tmp.add("http://antistorm.eu/currentImgs/estofex.png");

        return tmp;
    }

    private class BitmapTask extends AsyncTask<Integer, Void, ArrayList<Bitmap>> {

        @Override
        protected ArrayList<Bitmap> doInBackground(Integer... params) {
            ArrayList<String> nazwy = getFilesURLs(params[0]);
            ArrayList<Bitmap> lista = new ArrayList<Bitmap>();

            if (params[0] != null) {
                Bitmap tmp;
                for (int i = 0; i < 4; i++) {
                    tmp = getBitmapFromURL(nazwy.get(i));

                    if (tmp == null && i == 2)
                       tmp = velocity_blank;
                    else if (tmp == null)
                       tmp = blank;

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
                mapView.clear();
                Bitmap mapa = prepareBitmap(result);
                mapView.setImageBitmap(mapa);
                radar = probability = velocity = visual = estofex = null;
            } else if (result == null)
                error = true;
            if (postep != null)
                postep.dismiss();
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            postep = ProgressDialog.show(MapActivity.this, "Pobieranie", "Trwa pobieranie mapy ...", true, false);
        }
    }

    Paint preparePaint(){
        Paint p = new Paint();
        p.setAlpha(160);
        p.setAntiAlias(true);
        p.setFilterBitmap(true);
        p.setDither(true);
        return p;
    }

    Bitmap prepareBitmap(ArrayList<Bitmap> results){
        //radar = results.get(0);
        probability = results.get(0);
        velocity = results.get(1);
        visual = results.get(2);
        estofex = results.get(3);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;

        Bitmap mapa = BitmapFactory.decodeResource(getResources(), R.drawable.map, options);

        image = new Canvas();
        image.setBitmap(mapa);

        Paint p = preparePaint();
        Rect dest = new Rect(0,0,mapa.getWidth(), mapa.getHeight());

        //image.drawBitmap(radar, null, dest, p);
        p.setAlpha(100);
        image.drawBitmap(probability, null, dest, p);
        p.setAlpha(160);
        image.drawBitmap(visual, null, dest, p);
        image.drawBitmap(velocity, null, dest, p);
        image.drawBitmap(estofex, null, dest, p);
        return mapa;
    }

}
