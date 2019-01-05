package pl.revanmj.stormmonitor.logic;

import androidx.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/**
 * Created by revanmj on 06.11.2016.
 */

public class StormJobCreator implements JobCreator {

    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case StormJob.TAG:
                return new StormJob();
            default:
                return null;
        }
    }
}
