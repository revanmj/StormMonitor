package com.revanmj.stormmonitor.sql;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;
import com.revanmj.stormmonitor.model.StormData;

/**
 * Created by revanmj on 29.12.2013.
 */
public class CitiesAssetHelper extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "cities";
    private static final int DATABASE_VERSION = 2;

    public CitiesAssetHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public StormData getCity(String city_name) {

        SQLiteDatabase db = getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String [] sqlSelect = {"city_id", "name"};
        String sqlTables = "cities";
        String select = "name = '" + city_name +"'";

        qb.setTables(sqlTables);
        Cursor c = qb.query(db, sqlSelect, select, null,
                null, null, null);

        StormData city = null;
        if (c.moveToFirst()) {
                city = new StormData();
                city.setMiasto_id(Integer.parseInt(c.getString(0)));
                city.setMiasto(c.getString(1));
        }
        return city;

    }

    public Cursor searchCity(String city_name) {

        SQLiteDatabase db = getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String [] sqlSelect = {"city_id", "name"};
        String sqlTables = "cities";
        String select = "name LIKE '%" + city_name +"%'";

        qb.setTables(sqlTables);
        Cursor c = qb.query(db, sqlSelect, select, null,
                null, null, null);
        return c;

    }
}
