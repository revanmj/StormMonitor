package pl.revanmj.stormmonitor.logic;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.data.StormDataProvider;
import pl.revanmj.stormmonitor.model.StormData;

/**
 * Created by revanmj on 10.07.2013.
 */

public class Utils {
    private static String BASE_URL = "http://antistorm.eu/webservice.php?id=";

    static public List<StormData> getAllData(Context context){
        String[] projection = {StormDataProvider.KEY_ID, StormDataProvider.KEY_CITYNAME, StormDataProvider.KEY_STORMCHANCE, StormDataProvider.KEY_STORMTIME, StormDataProvider.KEY_RAINCHANCE, StormDataProvider.KEY_RAINTIME};
        Cursor c = context.getContentResolver().query(StormDataProvider.CONTENT_URI, projection, null, null, null);

        if (c != null) {
            List<StormData> cities = new ArrayList<>();
            while (c.moveToNext()) {
                StormData tmp = new StormData();
                tmp.setCityId(c.getInt(StormDataProvider.CITYID));
                tmp.setCityName(c.getString(StormDataProvider.CITYNAME));
                tmp.setStormChance(c.getInt(StormDataProvider.STORMCHANCE));
                tmp.setStormTime(c.getInt(StormDataProvider.STORMTIME));
                //tmp.setStormAlert(c.getInt(StormDataProvider.STORMALERT));
                tmp.setRainChance(c.getInt(StormDataProvider.RAINCHANCE));
                tmp.setRainTime(c.getInt(StormDataProvider.RAINTIME));
                //tmp.setRainAlert(c.getInt(StormDataProvider.RAINALERT));
                cities.add(tmp);
            }
            return cities;
        }

        return new ArrayList<>();
    }

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
        Log.d("makeHttpRequest", "GET: url[" + url_s + "], responseCode[" + responseCode + "]");

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

        public JsonObject getAsJsonObject() {
            JsonElement element = new JsonParser().parse(result);
            if (result != null && element.isJsonObject())
                return element.getAsJsonObject();
            else
                return null;
        }

    }

    static public int getStormData(List<StormData> list, Context context) {
        List<StormData> resultList = new ArrayList<>();
        int resultCode = -1;
        for (StormData city : list) {
            try {
                HttpResult result = makeHttpGetRequest(BASE_URL + city.getCityId());
                if (result.getResult() != null) {
                    JsonObject json = result.getAsJsonObject();

                    // Setting city database id
                    StormData data = new StormData();
                    data.setCityId(city.getCityId());
                    data.setCityName(json.get("m").getAsString());
                    data.setStormChance(json.get("p_b").getAsInt());
                    data.setStormTime(json.get("t_b").getAsInt());
                    data.setStormAlert(json.get("a_b").getAsInt());
                    data.setRainChance(json.get("p_o").getAsInt());
                    data.setRainTime(json.get("t_o").getAsInt());
                    data.setRainAlert(json.get("a_o").getAsInt());

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
                String selection = StormDataProvider.KEY_ID + " = ?";
                String[] selArgs = {Integer.toString(city.getCityId())};
                context.getContentResolver().update(StormDataProvider.CONTENT_URI, cv, selection, selArgs);
            }
            Log.d("getStormData", "result: " + resultList);
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