package com.revanmj.stormmonitor.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.revanmj.stormmonitor.model.StormData;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by revanmj on 26.12.2013.
 */
public class StormOpenHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "dictionary";
    private static final String TABLE_STORMS = "StormData";
    private static final String KEY_ID = "city_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_PBURZY = "p_burzy";
    private static final String KEY_TBURZY = "t_burzy";
    private static final String KEY_TOPADOW = "t_opadow";
    private static final String KEY_POPADOW = "p_opadow";
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_STORMS + " ( " +
                    KEY_ID + " INTEGER PRIMARY KEY, " +
                    KEY_NAME + " TEXT, "+
                    KEY_PBURZY + " INTEGER," +
                    KEY_TBURZY + " INTEGER," +
                    KEY_POPADOW + " INTEGER, " +
                    KEY_TOPADOW + " INTEGER )";

    public StormOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i2) {
        List<StormData> cities = new LinkedList<StormData>();

        String query = "SELECT  * FROM " + TABLE_STORMS;

        Cursor cursor = db.rawQuery(query, null);

        StormData city = null;
        if (cursor.moveToFirst()) {
            do {
                city = new StormData();
                city.setMiasto_id(Integer.parseInt(cursor.getString(0)));
                city.setMiasto(cursor.getString(1));
                city.setP_burzy(Integer.parseInt(cursor.getString(2)));
                city.setT_burzy(Integer.parseInt(cursor.getString(3)));
                city.setP_opadow(Integer.parseInt(cursor.getString(4)));
                city.setT_opadow(Integer.parseInt(cursor.getString(5)));

                cities.add(city);
            } while (cursor.moveToNext());
        }

        // Drop older books table if existed
        db.execSQL("DROP TABLE IF EXISTS StormData");

        // create fresh books table
        this.onCreate(db);

        for (int j=0; j<cities.size(); j++) {
            ContentValues values = new ContentValues();
            values.put(KEY_ID, cities.get(i).getMiasto_id());
            values.put(KEY_NAME, cities.get(i).getMiasto());
            values.put(KEY_PBURZY, cities.get(i).getP_burzy());
            values.put(KEY_TBURZY, cities.get(i).getT_burzy());
            values.put(KEY_POPADOW, 0);
            values.put(KEY_TOPADOW, 255);

            // 3. insert
            db.insert(TABLE_STORMS, // table
                    null, //nullColumnHack
                    values); // key/value -> keys = column names/ values = column values
        }

        // 4. close
        db.close();
    }

    public void addCity(StormData city){
        //for logging
        Log.d("addCity", city.toString());

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_ID, city.getMiasto_id());
        values.put(KEY_NAME, city.getMiasto());
        values.put(KEY_PBURZY, city.getP_burzy());
        values.put(KEY_TBURZY, city.getT_burzy());
        values.put(KEY_POPADOW, city.getP_opadow());
        values.put(KEY_TOPADOW, city.getT_opadow());

        // 3. insert
        db.insert(TABLE_STORMS, // table
                null, //nullColumnHack
                values); // key/value -> keys = column names/ values = column values

        // 4. close
        db.close();
    }

    public List<StormData> getAllCities() {
        List<StormData> cities = new LinkedList<StormData>();

        // 1. build the query
        String query = "SELECT  * FROM " + TABLE_STORMS;

        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        // 3. go over each row, build book and add it to list
        StormData city = null;
        if (cursor.moveToFirst()) {
            do {
                city = new StormData();
                city.setMiasto_id(Integer.parseInt(cursor.getString(0)));
                city.setMiasto(cursor.getString(1));
                city.setP_burzy(Integer.parseInt(cursor.getString(2)));
                city.setT_burzy(Integer.parseInt(cursor.getString(3)));
                city.setP_opadow(Integer.parseInt(cursor.getString(4)));
                city.setT_opadow(Integer.parseInt(cursor.getString(5)));

                // Add book to books
                cities.add(city);
            } while (cursor.moveToNext());
        }

        Log.d("getAllCities()", cities.toString());

        // return books
        return cities;
    }

    public int updateCity(StormData city) {

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, city.getMiasto());
        values.put(KEY_PBURZY, city.getP_burzy());
        values.put(KEY_TBURZY, city.getT_burzy());
        values.put(KEY_POPADOW, city.getP_opadow());
        values.put(KEY_TOPADOW, city.getT_opadow());

        // 3. updating row
        int i = db.update(TABLE_STORMS, //table
                values, // column/value
                KEY_ID + " = ?", // selections
                new String[] { String.valueOf(city.getMiasto_id()) }); //selection args

        // 4. close
        db.close();

        return i;

    }

    public void deleteCity(StormData city) {

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. delete
        db.delete(TABLE_STORMS, //table name
                KEY_ID+" = ?",  // selections
                new String[] { String.valueOf(city.getMiasto_id()) }); //selections args

        // 3. close
        db.close();

        //log
        Log.d("deleteCity", city.toString());

    }

}
