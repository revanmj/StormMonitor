package com.revanmj.stormmonitor;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class About extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        getSupportActionBar().setTitle("O aplikacji");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView wersja = (TextView) findViewById(R.id.textView3);

        PackageManager manager = this.getPackageManager();
        PackageInfo info;
        try {
            info = manager.getPackageInfo(this.getPackageName(), 0);
            wersja.setText(info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
    
}
