package com.revanmj.stormmonitor.logic;

import org.json.JSONObject;
import org.json.JSONException;
import com.revanmj.stormmonitor.model.StormData;

/**
 * Created by Student on 10.07.13.
 */
public class JSONparser {
    public static StormData getStormData(String data) throws JSONException  {
        StormData d_burze = new StormData();

        // We create out JSONObject from the data
        JSONObject jObj = new JSONObject(data);

        // We start extracting the info
        d_burze.setMiasto(getString("m",jObj));
        d_burze.setP_burzy(getInt("p_b", jObj));
        d_burze.setT_burzy(getInt("t_b",jObj));
        d_burze.setP_opadow(getInt("p_o", jObj));
        d_burze.setT_opadow(getInt("t_o", jObj));

        return d_burze;
    }

    private static JSONObject getObject(String tagName, JSONObject jObj)  throws JSONException {
        JSONObject subObj = jObj.getJSONObject(tagName);
        return subObj;
    }

    private static String getString(String tagName, JSONObject jObj) throws JSONException {
        return jObj.getString(tagName);
    }

    private static int  getInt(String tagName, JSONObject jObj) throws JSONException {
        return jObj.getInt(tagName);
    }
}
