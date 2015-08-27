package pl.revanmj.stormmonitor.logic;

import android.os.NetworkOnMainThreadException;
import android.os.StrictMode;
import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by revanmj on 28.07.2013.
 */
public class CheckConnection {

    static public boolean isHttpsAvalable(String url){
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        boolean responded = false;

        try {
            URL address = new URL(url);
            URLConnection conn = address.openConnection();
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(5000);

            responded = true;
        } catch (MalformedURLException e) {
            Log.e("pl.revanmj.StormMonitor", "Unable to connect to " + url + " " + e.toString());
        } catch (IOException e) {
            Log.e("pl.revanmj.StormMonitor", "Unable to connect to " + url + " " + e.toString());
            e.printStackTrace();
        } catch (NetworkOnMainThreadException e) {
            Log.e("pl.revanmj.StormMonitor", "Unable to connect to " + url + " " + e.toString());
        }
        return responded;
    }
}
