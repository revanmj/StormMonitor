package com.revanmj.stormmonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Calendar;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

public class MapActivity extends Activity {
    Bitmap radar, probability, visual, velocity, velocity_blank, estofex, blank;
    Canvas image;
    static TextView timeR;
    ImageViewTouch mapView;
    ProgressDialog postep;
    boolean error;

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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_map_refresh) {
            RefreshMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        String[] adresy = new String[4];
        //adresy[0] = "http://antistorm.eu/radar/radar.png";
        adresy[3] = "http://antistorm.eu/currentImgs/estofex.png";
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR), month = c.get(Calendar.MONTH) + 1, day = c.get(Calendar.DAY_OF_MONTH), hour = c.get(Calendar.HOUR_OF_DAY), minutes = c.get(Calendar.MINUTE);
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
        String timeS = "";
        if (month < 10)
            timeS = day + ".0" + month + "." + year + " " + hour + ":";
        else
            timeS = day + "." + month + "." + year + " " + hour + ":";
        if (minutes <10)
            timeS = timeS + "0" + minutes;
        else
            timeS = timeS + minutes;
        int minutes2 = minutes + 1;

        timeR = (TextView) findViewById(R.id.timeStamp);
        timeR.setText(timeS);
        adresy[0] = "http://antistorm.eu/archive/" + year + "." + month + "." + day +"/" + hour + "-" + minutes2 + "-radar-probabilitiesImg.png";
        adresy[1] = "http://antistorm.eu/archive/" + year + "." + month + "." + day +"/" + hour + "-" + minutes2 + "-radar-velocityMapImg.png";
        if (month < 10)
            adresy[2] = "http://antistorm.eu/visualPhenom/" + year + "0" + month  + day +"." + hour + minutes + "-radar-visualPhenomenon.png";
        else
            adresy[2] = "http://antistorm.eu/visualPhenom/" + year + month  + day +"." + hour + minutes + "-radar-visualPhenomenon.png";
        BitmapTask task = new BitmapTask();
        task.execute(adresy);
    }

    private class BitmapTask extends AsyncTask<String, Void, ArrayList<Bitmap>> {

        @Override
        protected ArrayList<Bitmap> doInBackground(String... params) {
            ArrayList<Bitmap> lista = new ArrayList<Bitmap>();
            if (params[0] != null) {
                int rozmiar = params.length;
                Bitmap tmp;
                for (int i = 0; i < rozmiar; i++) {
                    tmp = getBitmapFromURL(params[i]);
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
                //radar = result.get(0);
                probability = result.get(0);
                velocity = result.get(1);
                visual = result.get(2);
                estofex = result.get(3);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                Bitmap mapa = BitmapFactory.decodeResource(getResources(), R.drawable.map, options);
                Rect dest = new Rect(0,0,mapa.getWidth(), mapa.getHeight());
                Paint p = new Paint();
                p.setAlpha(160);
                p.setAntiAlias(true);
                p.setFilterBitmap(true);
                p.setDither(true);
                image = new Canvas();
                image.setBitmap(mapa);
                //image.drawBitmap(radar, null, dest, p);
                p.setAlpha(100);
                image.drawBitmap(probability, null, dest, p);
                p.setAlpha(160);
                image.drawBitmap(visual, null, dest, p);
                image.drawBitmap(velocity, null, dest, p);
                image.drawBitmap(estofex, null, dest, p);
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

}
