package com.revanmj.stormmonitor.logic;

import android.os.NetworkOnMainThreadException;
import android.os.StrictMode;
import android.util.Log;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;

/**
 * Created by revanmj on 28.07.2013.
 */
public class CheckConnection {

    static public boolean isHttpsAvalable(String url){
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        boolean responded = false;
        HttpGet requestTest = new HttpGet(url);
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 3000);
        HttpConnectionParams.setSoTimeout(params, 5000);
        DefaultHttpClient client = new DefaultHttpClient(params);
        try {
            client.execute(requestTest);
            responded = true;
        } catch (ClientProtocolException e) {
            Log.e("com.revanmj.StormMonitor", "Unable to connect to " + url + " " + e.toString());
        } catch (IOException e) {
            Log.e("com.revanmj.StormMonitor", "Unable to connect to " + url + " " + e.toString());
            e.printStackTrace();
        } catch (NetworkOnMainThreadException e) {
            Log.e("com.revanmj.StormMonitor", "Unable to connect to " + url + " " + e.toString());
        }
        return responded;
    }
}
