package pl.revanmj.stormmonitor;


import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;

import pl.revanmj.stormmonitor.data.SharedSettings;
import pl.revanmj.stormmonitor.logic.AppCompatPreferenceActivity;
import pl.revanmj.stormmonitor.logic.StormJob;

/**
 * Created by revanmj on 13.11.2016.
 */

public class PreferencesActivity extends AppCompatPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        final ListPreference syncPeriod = (ListPreference) findPreference(SharedSettings.SYNC_PERIOD);

        syncPeriod.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Integer period = Integer.parseInt((String) newValue);
                StormJob.scheduleJob(period);
                return true;
            }
        });

        int[] widgetIDs = AppWidgetManager.getInstance(this.getApplicationContext())
                .getAppWidgetIds(new ComponentName(this.getApplicationContext(), CitiesWidget.class));
        if (widgetIDs.length < 1) {
            syncPeriod.setEnabled(false);
        }

        Preference appVersion = findPreference("app_version");
        PackageManager manager = this.getPackageManager();
        PackageInfo info;
        try {
            info = manager.getPackageInfo(this.getPackageName(), 0);
            appVersion.setTitle("Wersja " + info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
