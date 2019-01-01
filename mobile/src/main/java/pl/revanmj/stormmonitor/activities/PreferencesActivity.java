package pl.revanmj.stormmonitor.activities;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import pl.revanmj.stormmonitor.CitiesWidget;
import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.data.SharedSettings;
import pl.revanmj.stormmonitor.logic.StormWorker;
import pl.revanmj.stormmonitor.logic.Utils;

/**
 * Created by revanmj on 13.11.2016.
 */

public class PreferencesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainPreferenceFragment mainFragment = new MainPreferenceFragment();
        mainFragment.setContext(this);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, mainFragment).commit();
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

    public static class MainPreferenceFragment extends PreferenceFragmentCompat {
        private Context mContext;

        public void setContext(Context context) {
            mContext = context;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.preferences);

            final ListPreference syncPeriod = (ListPreference) findPreference(SharedSettings.SYNC_PERIOD);
            syncPeriod.setOnPreferenceChangeListener((preference, newValue) -> {
                Integer period = Integer.parseInt((String) newValue);
                StormWorker.scheduleJob(period);
                return true;
            });

            int[] widgetIDs = AppWidgetManager.getInstance(mContext)
                    .getAppWidgetIds(new ComponentName(mContext, CitiesWidget.class));
            if (widgetIDs.length < 1) {
                syncPeriod.setEnabled(false);
            }

            Preference appVersion = findPreference("app_version");
            appVersion.setTitle(getString(R.string.settings_version, Utils.APP_VERSION));
        }
    }
}
