package pl.revanmj.stormmonitor.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/**
 * Created by revanmj on 29.12.2013.
 */

public class CitiesAssetHelper extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "cities";
    private static final int DATABASE_VERSION = 2;
    public static final String CITY_ID = "city_id";
    public static final String CITY_NAME = "name";
    public static final String CITIES_TABLE = "cities";

    public CitiesAssetHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public StormData getCity(String city_name) {

        SQLiteDatabase db = getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String [] select = {CITY_ID, CITY_NAME};
        String where = CITY_NAME + " = '" + city_name +"'";

        qb.setTables(CITIES_TABLE);
        Cursor c = qb.query(db, select, where, null,
                null, null, null);

        StormData city = null;
        if (c.moveToFirst()) {
                city = new StormData();
                city.setCityId(Integer.parseInt(c.getString(0)));
                city.setCityName(c.getString(1));
        }
        return city;

    }

    public Cursor getAllCities() {

        SQLiteDatabase db = getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String [] sqlSelect = {CITY_ID, CITY_NAME};

        qb.setTables(CITIES_TABLE);
        Cursor c = qb.query(db, sqlSelect, null, null,
                null, null, null);
        return c;

    }
}
