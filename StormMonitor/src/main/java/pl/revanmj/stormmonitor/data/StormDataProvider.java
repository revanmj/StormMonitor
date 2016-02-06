package pl.revanmj.stormmonitor.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import pl.revanmj.stormmonitor.model.StormData;

/**
 * Created by revanmj on 05.02.2016.
 */

public class StormDataProvider extends ContentProvider {
    public static int CITYID = 0;
    public static int CITYNAME = 1;
    public static int STORMCHANCE = 2;
    public static int STORMTIME = 3;
    public static int RAINCHANCE = 4;
    public static int RAINTIME = 5;

    public static final String PROVIDER_NAME = "pl.revanmj.provider.StormData";
    public static final String URL = "content://" + PROVIDER_NAME + "/cities";
    public static final Uri CONTENT_URI = Uri.parse(URL);
    private static HashMap<String, String> CITIES_PROJECTION_MAP;

    static final int URI_CITIES = 1;
    static final int URI_CITYID = 2;

    static final UriMatcher uriMatcher;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "cities", URI_CITIES);
        uriMatcher.addURI(PROVIDER_NAME, "cities/#", URI_CITYID);
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        StormDataSqlHelper dbHelper = new StormDataSqlHelper(context);

        db = dbHelper.getWritableDatabase();
        return (db != null);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE_STORMS);

        switch (uriMatcher.match(uri)) {
            case URI_CITIES:
                qb.setProjectionMap(CITIES_PROJECTION_MAP);
                break;

            case URI_CITYID:
                qb.appendWhere( CITYID + "=" + uri.getPathSegments().get(1));
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (sortOrder == null || sortOrder == ""){
            // By default sort on cities names
            sortOrder = KEY_CITYNAME;
        }
        Cursor c = qb.query(db,	projection,	selection, selectionArgs,null, null, sortOrder);

        // register to watch a content URI for changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)){
            // Get all cities
            case URI_CITIES:
                return "vnd.android.cursor.dir/vnd.revanmj.cities";

            // Get specific city
            case URI_CITYID:
                return "vnd.android.cursor.item/vnd.revanmj.cities";

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        long rowId = db.insert(TABLE_STORMS, null, contentValues);

        if (rowId > 0) {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }

        throw new SQLException("Failed to add a city into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){
            case URI_CITIES:
                count = db.delete(TABLE_STORMS, selection, selectionArgs);
                break;

            case URI_CITYID:
                String id = uri.getPathSegments().get(1);
                count = db.delete(TABLE_STORMS, CITYID +  " = " + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){
            case URI_CITIES:
                count = db.update(TABLE_STORMS, values, selection, selectionArgs);
                break;

            case URI_CITYID:
                count = db.update(TABLE_STORMS, values, CITYID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" +selection + ')' : ""), selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri );
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private SQLiteDatabase db;
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "dictionary";
    private static final String TABLE_STORMS = "StormData";
    public static final String KEY_ID = "city_id";
    public static final String KEY_CITYNAME = "name";
    public static final String KEY_STORMCHANCE = "p_burzy";
    public static final String KEY_STORMTIME = "t_burzy";
    public static final String KEY_RAINTIME = "t_opadow";
    public static final String KEY_RAINCHANCE = "p_opadow";
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_STORMS + " ( " +
                    KEY_ID + " INTEGER PRIMARY KEY, " +
                    KEY_CITYNAME + " TEXT, "+
                    KEY_STORMCHANCE + " INTEGER," +
                    KEY_STORMTIME + " INTEGER," +
                    KEY_RAINCHANCE + " INTEGER, " +
                    KEY_RAINTIME + " INTEGER )";

    public static class StormDataSqlHelper extends SQLiteOpenHelper {

        public StormDataSqlHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int i, int i1) {
            List<StormData> cities = new LinkedList<StormData>();

            String query = "SELECT  * FROM " + TABLE_STORMS;

            Cursor cursor = db.rawQuery(query, null);

            StormData city = null;
            if (cursor.moveToFirst()) {
                do {
                    city = new StormData();
                    city.setCityId(Integer.parseInt(cursor.getString(0)));
                    city.setCityName(cursor.getString(1));
                    city.setStormChance(Integer.parseInt(cursor.getString(2)));
                    city.setStormTime(Integer.parseInt(cursor.getString(3)));
                    city.setRainChance(Integer.parseInt(cursor.getString(4)));
                    city.setRainTime(Integer.parseInt(cursor.getString(5)));

                    cities.add(city);
                } while (cursor.moveToNext());
            }

            // Drop older table if existed
            db.execSQL("DROP TABLE IF EXISTS StormData");

            // create fresh table
            this.onCreate(db);

            for (int j=0; j<cities.size(); j++) {
                ContentValues values = new ContentValues();
                values.put(KEY_ID, cities.get(i).getCityId());
                values.put(KEY_CITYNAME, cities.get(i).getCityName());
                values.put(KEY_STORMCHANCE, cities.get(i).getStormChance());
                values.put(KEY_STORMTIME, cities.get(i).getStormTime());
                values.put(KEY_RAINCHANCE, 0);
                values.put(KEY_RAINTIME, 255);

                // 3. insert
                db.insert(TABLE_STORMS, null, values);
            }

            db.close();
        }
    }
}
