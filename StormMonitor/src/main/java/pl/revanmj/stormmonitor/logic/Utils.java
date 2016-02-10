package pl.revanmj.stormmonitor.logic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.JsonReader;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

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
                tmp.setRainChance(c.getInt(StormDataProvider.RAINCHANCE));
                tmp.setRainTime(c.getInt(StormDataProvider.RAINTIME));
                cities.add(tmp);
            }
            return cities;
        }

        return new ArrayList<>();
    }

    static public int getStormData(List<StormData> list, Context context) {
        int resultCode = 1;
        int responseCode = -1;
        HttpURLConnection con = null;
        List<StormData> resultList = new ArrayList<>();

        for (StormData city : list) {
            try {
                con = (HttpURLConnection) (new URL(BASE_URL + city.getCityId())).openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(3000);
                con.connect();
                responseCode = con.getResponseCode();

                JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream(), "UTF-8"));

                // Setting city database id
                StormData data = new StormData();
                data.setCityId(city.getCityId());

                // Parsing received data into StormData obejct
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals("m"))
                        data.setCityName(reader.nextString());
                    else if (name.equals("p_b"))
                        data.setStormChance(reader.nextInt());
                    else if (name.equals("t_b"))
                        data.setStormTime(reader.nextInt());
                    else if (name.equals("p_o"))
                        data.setRainChance(reader.nextInt());
                    else if (name.equals("t_o"))
                        data.setRainTime(reader.nextInt());
                    else {
                        reader.skipValue();
                    }
                }
                reader.endObject();

                // Adding result to a list
                resultList.add(data);

            } catch (UnknownHostException e) {
                // Couldn't connect to a host, so assume we have no internet connection (or host is down)
                resultCode = 2;
            } catch (Exception e) {
                // Unknown exception. If HTTP status code is available, pass it further
                if (responseCode > 99 && responseCode < 600)
                    resultCode = responseCode;
                else {
                    resultCode = -1;
                    e.printStackTrace();
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Create object with final list or just an error code
        if (resultCode == 1) {
            for (StormData city : resultList) {
                ContentValues cv = new ContentValues();
                cv.put(StormDataProvider.KEY_STORMCHANCE, city.getStormChance());
                cv.put(StormDataProvider.KEY_STORMTIME, city.getStormTime());
                cv.put(StormDataProvider.KEY_RAINCHANCE, city.getRainChance());
                cv.put(StormDataProvider.KEY_RAINTIME, city.getRainTime());
                String selection = StormDataProvider.KEY_ID + " = ?";
                String[] selArgs = {Integer.toString(city.getCityId())};
                context.getContentResolver().update(StormDataProvider.CONTENT_URI, cv, selection, selArgs);
            }
            return 1;
        }

        return resultCode;
    }
}