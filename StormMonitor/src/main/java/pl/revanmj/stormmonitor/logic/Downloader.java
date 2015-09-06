package pl.revanmj.stormmonitor.logic;

import android.util.JsonReader;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import pl.revanmj.stormmonitor.model.DownloadResult;
import pl.revanmj.stormmonitor.model.StormData;

/**
 * Created by Student on 10.07.13.
 */
public class Downloader {
    private static String BASE_URL = "http://antistorm.eu/webservice.php?id=";


    static public DownloadResult getStormData(List<StormData> list) {
        int resultCode = 1;
        int responseCode = -1;
        HttpURLConnection con = null;
        List<StormData> resultList = new ArrayList<>();

        for (StormData city : list) {
            try {
                con = (HttpURLConnection) (new URL(BASE_URL + city.getMiasto_id())).openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(3000);
                con.connect();
                responseCode = con.getResponseCode();

                JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream(), "UTF-8"));

                // Setting city database id
                StormData data = new StormData();
                data.setMiasto_id(city.getMiasto_id());

                // Parsing received data into StormData obejct
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals("m"))
                        data.setMiasto(reader.nextString());
                    else if (name.equals("p_b"))
                        data.setP_burzy(reader.nextInt());
                    else if (name.equals("t_b"))
                        data.setT_burzy(reader.nextInt());
                    else if (name.equals("p_o"))
                        data.setP_opadow(reader.nextInt());
                    else if (name.equals("t_o"))
                        data.setT_opadow(reader.nextInt());
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
                // Unknown exception. If HTTP status code is available, pass it
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
        if (resultCode == 1)
            return new DownloadResult(resultList);
        else
            return new DownloadResult(resultCode);
    }
}