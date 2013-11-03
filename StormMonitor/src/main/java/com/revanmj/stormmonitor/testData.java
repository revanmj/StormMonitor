package com.revanmj.stormmonitor;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;

import java.util.Locale;

public class testData extends ActionBarActivity {

    private TextView cityText;
    private TextView prawdText;
    private TextView czasText;
    private Button przycisk;
    private EditText pole;
    private InputMethodManager imm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        getSupportActionBar().setTitle("Test");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        cityText = (TextView) findViewById(R.id.textView);
        prawdText = (TextView) findViewById(R.id.textView2);
        czasText = (TextView) findViewById(R.id.textView3);
        przycisk = (Button) findViewById(R.id.button);
        pole = (EditText) findViewById(R.id.editText);

        View.OnClickListener btnListener = new View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                JSONStormTask task = new JSONStormTask();
                task.execute(pole.getText().toString());
                pole.getText().clear();
            }
        };
        przycisk.setOnClickListener(btnListener);

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if (imm != null)
                    imm.toggleSoftInput(InputMethodManager.RESULT_UNCHANGED_HIDDEN, 0);
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setData(StormData result)
    {
        cityText.setText(result.getMiasto());
        prawdText.setText("Szansa na burz\u0119: " + result.getP_burzy() + " / 255");
        if (result.getT_burzy() < 240)
            czasText.setText("Czas do burzy: ~" + result.getT_burzy() + " min");
        else
            czasText.setText("");
    }

    private class JSONStormTask extends AsyncTask<String, Void, StormData> {

        protected ProgressDialog postep;

        @Override
        protected StormData doInBackground(String... params) {
            StormData stormData = new StormData();
            String data = ( (new Downloader()).getStormData(Integer.parseInt(params[0])));

            try {
                stormData = JSONparser.getStormData(data);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return stormData;

        }

        @Override
        protected void onPostExecute(StormData result) {
            setData(result);
            postep.dismiss();
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            //postep = new ProgressDialog();
            postep = ProgressDialog.show(testData.this, "Pobieranie", "Trwa pobieranie danych ...", true, false);
        }
    }
    
}
