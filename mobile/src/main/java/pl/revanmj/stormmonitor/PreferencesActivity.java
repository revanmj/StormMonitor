package pl.revanmj.stormmonitor;


import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.view.MenuItem;

import pl.revanmj.stormmonitor.data.SharedSettings;
import pl.revanmj.stormmonitor.logic.AppCompatPreferenceActivity;
import pl.revanmj.stormmonitor.logic.StormJob;
import pl.revanmj.stormmonitor.logic.Utils;

/**
 * Created by revanmj on 13.11.2016.
 */

public class PreferencesActivity extends AppCompatPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        final ListPreference syncPeriod = (ListPreference) findPreference(SharedSettings.SYNC_PERIOD);
        syncPeriod.setOnPreferenceChangeListener((preference, newValue) -> {
            Integer period = Integer.parseInt((String) newValue);
            StormJob.scheduleJob(period);
            return true;
        });

        int[] widgetIDs = AppWidgetManager.getInstance(this.getApplicationContext())
                .getAppWidgetIds(new ComponentName(this.getApplicationContext(), CitiesWidget.class));
        if (widgetIDs.length < 1) {
            syncPeriod.setEnabled(false);
        }

        Preference appVersion = findPreference("app_version");
        appVersion.setTitle(getString(R.string.settings_version, Utils.APP_VERSION));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()) {
            case android.R.id.home:
                PreferencesActivity.this.onBackPressed();
                break;
        }
        return true;
    }

}
