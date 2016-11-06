package pl.revanmj.stormmonitor;

import android.app.Application;

import com.evernote.android.job.JobManager;

import pl.revanmj.stormmonitor.logic.StormJobCreator;

/**
 * Created by revanmj on 06.11.2016.
 */

public class StormApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        JobManager.create(this).addJobCreator(new StormJobCreator());
    }
}
