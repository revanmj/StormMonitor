package pl.revanmj.stormmonitor.adapters;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

import pl.revanmj.stormmonitor.sql.CitiesAssetHelper;

/**
 * Created by revanmj on 26.07.2015.
 */
public class SearchProvider extends ContentProvider {
    private CitiesAssetHelper mDB;
    private static final String AUTHORITY = "pl.revanmj.stormmonitor.data.CitiesProvider";
    public static final int CITIES = 100;
    public static final int CITIES_ID = 110;
    private MatrixCursor asyncCursor;

    private static final String CITIES_BASE_PATH = "cities";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CITIES_BASE_PATH);

    private static final int SEARCH_SUGGEST = 1;

    private static final String[] SEARCH_SUGGEST_COLUMNS = {
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA
    };

    @Override
    public boolean onCreate() {
        mDB = new CitiesAssetHelper(getContext());
        asyncCursor = new MatrixCursor(SEARCH_SUGGEST_COLUMNS, 10);
        return true;
    }

    private static final UriMatcher sURIMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        sURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(CitiesAssetHelper.CITIES_TABLE);

        Cursor cursor = queryBuilder.query(mDB.getReadableDatabase(),
                projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        MatrixCursor nCursor = new MatrixCursor(SEARCH_SUGGEST_COLUMNS, 10);
        do {
            nCursor.addRow(new String[] {cursor.getString(0), cursor.getString(1), cursor.getString(0)});
        }while (cursor.moveToNext());

        return nCursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        throw new UnsupportedOperationException();
    }


}
