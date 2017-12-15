package pl.revanmj.stormmonitor.logic;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import pl.revanmj.stormmonitor.BuildConfig;
import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.data.StormDataProvider;
import pl.revanmj.stormmonitor.model.StormData;

/**
 * Created by revanmj on 10.07.2013.
 */

public class Utils {
    private static final String LOG_TAG = Utils.class.getSimpleName();

    public static final String APP_VERSION = BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";

    private final static String BASE_URL = "http://antistorm.eu/webservice.php?id=";

    public final static String STORM_FILENAME = "finalStormMap.jpg";
    public final static String RAIN_FILENAME = "finalRainMap.jpg";


    static public List<StormData> getAllData(Context context){
        Cursor c = context.getContentResolver().query(StormDataProvider.CONTENT_URI, null, null, null, null);

        if (c != null) {
            List<StormData> cities = new ArrayList<>();
            while (c.moveToNext()) {
                StormData tmp = new StormData();
                tmp.setCityId(c.getInt(c.getColumnIndex(StormDataProvider.KEY_ID)));
                tmp.setCityName(c.getString(c.getColumnIndex(StormDataProvider.KEY_CITYNAME)));
                tmp.setStormChance(c.getInt(c.getColumnIndex(StormDataProvider.KEY_STORMCHANCE)));
                tmp.setStormTime(c.getInt(c.getColumnIndex(StormDataProvider.KEY_STORMTIME)));
                tmp.setStormAlert(c.getInt(c.getColumnIndex(StormDataProvider.KEY_STORMALERT)));
                tmp.setRainChance(c.getInt(c.getColumnIndex(StormDataProvider.KEY_RAINCHANCE)));
                tmp.setRainTime(c.getInt(c.getColumnIndex(StormDataProvider.KEY_RAINTIME)));
                tmp.setRainAlert(c.getInt(c.getColumnIndex(StormDataProvider.KEY_RAINALERT)));
                cities.add(tmp);
            }
            c.close();
            return cities;
        }

        return new ArrayList<>();
    }

    @WorkerThread
    public static HttpResult makeHttpGetRequest(String url_s) throws IOException {
        URL url = new URL(url_s);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Connection", "close");
        connection.setConnectTimeout(6000);
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode >= 400)
            return new HttpResult(responseCode, null);

        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
        Log.d(LOG_TAG, "GET: url[" + url_s + "], responseCode[" + responseCode + "]");

        StringBuilder result = new StringBuilder("");
        String line = "";
        while ((line = br.readLine()) != null) {
            result.append(line);
        }
        br.close();
        connection.disconnect();

        if (result.length() != 0)
            return new HttpResult(responseCode, result.toString());
        else
            return new HttpResult(responseCode, null);
    }

    public static class HttpResult {
        private int responseCode;
        private String result;

        public HttpResult(int code, String r) {
            responseCode = code;
            result = r;
        }

        public String getResult() {
            return result;
        }

        public int getResponseCode() {
            return responseCode;
        }
    }

    @WorkerThread
    static public int getStormData(List<StormData> list, Context context) {
        List<StormData> resultList = new ArrayList<>();
        int resultCode = -1;
        for (StormData city : list) {
            try {
                HttpResult result = makeHttpGetRequest(BASE_URL + city.getCityId());
                String json = result.getResult();

                if (json != null) {
                    Gson gson = new Gson();
                    StormData data = gson.fromJson(json, StormData.class);
                    data.setCityId(city.getCityId());

                    // Adding result to a list
                    resultList.add(data);
                } else {
                    resultCode = result.getResponseCode();
                }
            } catch (UnknownHostException e) {
                // Couldn't connect to a host, so assume we have no internet connection (or host is down)
                resultCode = 2;
            } catch (Exception e) {
                // Unknown exception.
                resultCode = -1;
                e.printStackTrace();
            }
        }

        if (resultList.size() > 0) {
            for (StormData city : resultList) {
                ContentValues cv = new ContentValues();
                cv.put(StormDataProvider.KEY_STORMCHANCE, city.getStormChance());
                cv.put(StormDataProvider.KEY_STORMTIME, city.getStormTime());
                cv.put(StormDataProvider.KEY_STORMALERT, city.getStormAlert());
                cv.put(StormDataProvider.KEY_RAINCHANCE, city.getRainChance());
                cv.put(StormDataProvider.KEY_RAINTIME, city.getRainTime());
                cv.put(StormDataProvider.KEY_RAINALERT, city.getRainAlert());
                context.getContentResolver().update(Uri.withAppendedPath(StormDataProvider.CONTENT_URI, Integer.toString(city.getCityId())),
                        cv, null, null);
            }
            Log.d(LOG_TAG, "result: " + resultList);
            return 1;
        }

        return resultCode;
    }

    static public int getRectColor(int stormTime, int stormChance, int rainTime, int rainChance) {
        if (stormTime <= 120 && stormTime > 60 && stormChance >= 10 || rainTime <= 120 && rainTime > 60 && rainChance >= 10)
            return R.drawable.rectangle_yellow;
        else if (stormTime <= 60 && stormTime > 30 && stormChance >= 10 || rainTime <= 60 && rainTime > 30 && rainChance >= 10)
            return R.drawable.rectangle_orange;
        else if (stormTime <= 30 && stormChance >= 30 || rainTime <= 30 && rainChance >= 30)
            return R.drawable.rectangle_red;
        else
            return R.drawable.rectangle_green;
    }

    static public String getTimeString(int time, int alert) {
        if (time < 240 && alert == 1) {
            return "~ " + time + " min";
        } else {
            return "-";
        }
    }

    static public String chromeChannel(Context ctx) {
        String chromeStable = "com.android.chrome";
        String chromeBeta = "com.chrome.beta";
        String chromeDev = "com.chrome.dev";

        if (isPackageInstalled(chromeStable, ctx))
            return chromeStable;
        if (isPackageInstalled(chromeBeta, ctx))
            return chromeBeta;
        if (isPackageInstalled(chromeDev, ctx))
            return chromeDev;

        return null;
    }

    static public boolean isPackageInstalled(String packagename, Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}