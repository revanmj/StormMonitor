package pl.revanmj.stormmonitor.model;

import java.util.List;

/**
 * Created by revanmj on 06.09.2015.
 */
public class DownloadResult {
    List<StormData> cities = null;
    int resultCode = -1;
    /**
     * Result codes:
     * -1 - unknown error
     *  1 - success
     *  2 - no internet connection
     */

    public DownloadResult(List<StormData> data) {
        cities = data;
        resultCode = 1;
    }

    public DownloadResult(int code) {
        resultCode = code;
    }

    public int getResultCode() {
        return resultCode;
    }

    public List<StormData> getCitiesData() {
        return cities;
    }
}
