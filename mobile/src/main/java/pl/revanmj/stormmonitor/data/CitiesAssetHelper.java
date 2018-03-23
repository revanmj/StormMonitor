package pl.revanmj.stormmonitor.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import pl.revanmj.stormmonitor.logic.Utils;

/**
 * Created by revanmj on 29.12.2013.
 */

public class CitiesAssetHelper extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "cities";
    private static final int DATABASE_VERSION = 2;
    private static final String CITY_ID = "city_id";
    private static final String CITY_NAME = "name";
    private static final String CITIES_TABLE = "cities";

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
                city = Utils.getCityFromCursor(c);
        }
        return city;

    }

    public Cursor searchCity(String city_name) {

        SQLiteDatabase db = getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String [] select = {CITY_ID, CITY_NAME};
        String where = null;
        if (city_name != null && !city_name.isEmpty())
            where = CITY_NAME + " LIKE '%" + city_name +"%'";

        qb.setTables(CITIES_TABLE);
        return qb.query(db, select, where, null, null, null, null);
    }
}
