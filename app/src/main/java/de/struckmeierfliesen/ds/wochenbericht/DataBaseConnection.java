package de.struckmeierfliesen.ds.wochenbericht;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class DataBaseConnection {
    // Database fields
    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;
    private String[] allEntriesColumns = {
            MySQLiteHelper.COLUMN_ID,
            MySQLiteHelper.COLUMN_CLIENT,
            MySQLiteHelper.COLUMN_DATE,
            MySQLiteHelper.COLUMN_DURATION,
            MySQLiteHelper.COLUMN_INSTALLER_ID,
            MySQLiteHelper.COLUMN_WORK,
    };

    private String[] allInstallersColumns = {
            MySQLiteHelper.INSTALLERS_COLUMN_ID,
            MySQLiteHelper.INSTALLERS_COLUMN_NAME
    };

    private BiMap<String, Integer> installers = HashBiMap.create();

    public DataBaseConnection(Context context) {
        dbHelper = new MySQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public String exportDatabase() {
        JSONArray entriesDb = tableToJSON(MySQLiteHelper.TABLE_ENTRIES);
        JSONArray installerDb = tableToJSON(MySQLiteHelper.TABLE_INSTALLERS);
        JSONObject databaseJSON = new JSONObject();
        String jsonString = "";
        try {
            databaseJSON.put(MySQLiteHelper.TABLE_ENTRIES, entriesDb);
            databaseJSON.put(MySQLiteHelper.TABLE_INSTALLERS, installerDb);
            jsonString = databaseJSON.toString();
            System.out.println(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

    private JSONArray tableToJSON(String tableName) {
        String searchQuery = "SELECT  * FROM " + tableName;
        Cursor cursor = database.rawQuery(searchQuery, null);

        JSONArray resultSet = new JSONArray();

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int totalColumn = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();
            for (int i = 0 ; i < totalColumn ; i++) {
                if (cursor.getColumnName(i) != null) {
                    try {
                        if (cursor.getString(i) != null) {
                            Log.d("TAG_NAME", cursor.getString(i));
                            rowObject.put(cursor.getColumnName(i), cursor.getString(i));
                        }
                        else {
                            rowObject.put(cursor.getColumnName(i), "");
                        }
                    }
                    catch (Exception e) {
                        Log.d("TAG_NAME", e.getMessage());
                    }
                }
            }
            resultSet.put(rowObject);
            cursor.moveToNext();
        }
        cursor.close();
        Log.d("TAG_NAME", resultSet.toString());
        return resultSet;
    }

    public static void dropDatabase(Context context) {
        context.deleteDatabase(MySQLiteHelper.DATABASE_NAME);
    }

    public void importDatabase(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            importTable(jsonObject, MySQLiteHelper.TABLE_ENTRIES);
            importTable(jsonObject, MySQLiteHelper.TABLE_INSTALLERS);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void importTable(JSONObject jsonObject, String tableName) {
        try {
            JSONArray jsonArray = jsonObject.getJSONArray(tableName);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject entry = jsonArray.getJSONObject(i);
                ContentValues values = new ContentValues();

                Iterator<String> iter = entry.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    try {
                        Object value = entry.get(key);
                        if (value instanceof String) values.put(key, (String) value);
                        if (value instanceof Integer) values.put(key, (int) value);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                database.insert(tableName, null, values);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Entry> getEntries(@Nullable Date date) {
        ArrayList<Entry> entries = new ArrayList<>();
        // TODO maybe check if update is necessary
        Cursor cursor = database.query(MySQLiteHelper.TABLE_ENTRIES, allEntriesColumns, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            // TODO check date earlier in query
            Entry entry = cursorToEntry(cursor, date);
            if(entry != null) entries.add(entry);
            cursor.moveToNext();
        }
        cursor.close();
        Collections.reverse(entries);
        return entries;
    }

    public List<List<Entry>> getLastWeekEntries(Date date) {
        Date[] lastWeek = Util.getDatesOfLastWeek(date);
        List<List<Entry>> weekEntries = new ArrayList<List<Entry>>(4);
        for (Date day : lastWeek) {
            ArrayList<Entry> entries = getEntries(day);
            weekEntries.add(entries);
        }
        return weekEntries;
    }

    private Entry cursorToEntry(Cursor cursor, @Nullable Date onlyDate) {
        if(cursor.getCount() == 0) return null;
        int id = cursor.getInt(cursor.getColumnIndex(MySQLiteHelper.COLUMN_ID));
        String client = cursor.getString(cursor.getColumnIndex(MySQLiteHelper.COLUMN_CLIENT));
        long time = (long) cursor.getInt(cursor.getColumnIndex(MySQLiteHelper.COLUMN_DATE)) * 1000;
        Date date = new Date(time);
        if(onlyDate != null && !Util.isSameDay(date, onlyDate)) return null;
        int duration = cursor.getInt(cursor.getColumnIndex(MySQLiteHelper.COLUMN_DURATION));
        int installerId = cursor.getInt(cursor.getColumnIndex(MySQLiteHelper.COLUMN_INSTALLER_ID));
        String work = cursor.getString(cursor.getColumnIndex(MySQLiteHelper.COLUMN_WORK));

        Entry entry = new Entry(
                client,
                date,
                duration,
                installerId,
                work
        );
        entry.id = id;
        return entry;
    }

    public int saveEntry(Entry entry) {
        if(entry.installerId == -1) throw new RuntimeException("You cannot add an entry without an installer!");
        ContentValues values = entryToValues(entry);
        return (int) database.insert(MySQLiteHelper.TABLE_ENTRIES, null, values);
    }

    public int addInstaller(String installer) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.INSTALLERS_COLUMN_NAME, installer);
        return (int) database.insert(MySQLiteHelper.TABLE_INSTALLERS, null, values);
    }

    public BiMap<String, Integer> getInstallers() {
        if(installers.size() == 0) {
            Cursor cursor = database.query(MySQLiteHelper.TABLE_INSTALLERS, allInstallersColumns, null, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                int installerId = cursor.getInt(cursor.getColumnIndex(MySQLiteHelper.INSTALLERS_COLUMN_ID));
                String installer = cursor.getString(cursor.getColumnIndex(MySQLiteHelper.INSTALLERS_COLUMN_NAME));
                installers.put(installer, installerId);
                cursor.moveToNext();
            }
            cursor.close();
        }
        return installers;
    }

    public ArrayList<Entry> idToInstaller(ArrayList<Entry> entries) {
        BiMap<Integer, String> biInstallers = getInstallers().inverse();

        for(Entry entry : entries) {
            entry.installer = biInstallers.get(entry.installerId);
        }
        return entries;
    }

    // not very efficient
    public Entry idToInstaller(Entry entry) {
        ArrayList<Entry> entryList = new ArrayList<Entry>();
        entryList.add(entry);
        return idToInstaller(entryList).get(0);
    }

    public ArrayList<Entry> getEntriesWithInstaller(@Nullable Date date) {
        return idToInstaller(getEntries(date));
    }

    public void editEntry(Entry entry) {
        ContentValues values = entryToValues(entry);
        if(entry.id != -1)
            database.update(MySQLiteHelper.TABLE_ENTRIES, values, MySQLiteHelper.COLUMN_ID + "=" + entry.id, null);
    }

    private ContentValues entryToValues(Entry entry) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_CLIENT, entry.client);
        long time = entry.date.getTime() / 1000;
        values.put(MySQLiteHelper.COLUMN_DATE, time);
        values.put(MySQLiteHelper.COLUMN_DURATION, entry.duration);
        values.put(MySQLiteHelper.COLUMN_INSTALLER_ID, entry.installerId);
        values.put(MySQLiteHelper.COLUMN_WORK, entry.work);
        return values;
    }

    // return true if exactly one row was removed
    public boolean deleteEntry(Entry entry) {
        return database.delete(MySQLiteHelper.TABLE_ENTRIES, MySQLiteHelper.COLUMN_ID + "=" + entry.id, null) == 1;
    }

    public Entry loadAverageEntry() {
        // actually i'm just going to load the last entry
        Cursor cursor = database.query(
                MySQLiteHelper.TABLE_ENTRIES, allEntriesColumns, null, null, null, null, MySQLiteHelper.COLUMN_DATE + " DESC", "1");
        cursor.moveToFirst();
        return (cursor.getCount() > 0) ? cursorToEntry(cursor, null) : null;
    }

    public boolean deleteInstaller(int installerId) {
        boolean installerDeleted = database.delete(MySQLiteHelper.TABLE_INSTALLERS, MySQLiteHelper.INSTALLERS_COLUMN_ID + "=" + installerId, null) == 1;
        boolean entriesDeleted = database.delete(MySQLiteHelper.TABLE_ENTRIES, MySQLiteHelper.COLUMN_INSTALLER_ID + "=" + installerId, null) == 1;
        return installerDeleted && entriesDeleted;
    }
}
