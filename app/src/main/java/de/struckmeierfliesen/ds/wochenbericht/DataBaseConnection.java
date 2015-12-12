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
import java.util.LinkedHashSet;
import java.util.List;

import static de.struckmeierfliesen.ds.wochenbericht.MySQLiteHelper.*;

public class DataBaseConnection {
    // Database fields
    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;

    private final String absoluteAllEntriesColumns =
            TABLE_ENTRIES + "." + COLUMN_ID + ", " +
            TABLE_ENTRIES + "." + COLUMN_DATE + ", " +
            TABLE_ENTRIES + "." + COLUMN_DURATION + ", " +
            TABLE_ENTRIES + "." + COLUMN_INSTALLER_ID + ", " +
            TABLE_ENTRIES + "." + COLUMN_WORK + ", " +
            TABLE_ENTRIES + "." + COLUMN_PICTURE_PATH;

    private final String[] allEntriesColumns = {
            COLUMN_ID,
            COLUMN_CLIENT,
            COLUMN_DATE,
            COLUMN_DURATION,
            COLUMN_INSTALLER_ID,
            COLUMN_WORK,
            COLUMN_PICTURE_PATH,
    };

    private final String[] allInstallersColumns = {
            INSTALLERS_COLUMN_ID,
            INSTALLERS_COLUMN_NAME
    };

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
            databaseJSON.put(TABLE_ENTRIES, entriesDb);
            databaseJSON.put(TABLE_INSTALLERS, installerDb);
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
        context.deleteDatabase(DATABASE_NAME);
    }

    public void importDatabase(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            importTable(jsonObject, TABLE_ENTRIES);
            importTable(jsonObject, TABLE_INSTALLERS);
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
    private List<Entry> getEntries(@Nullable Date date) {
        return getEntries(date, null);
    }

    private List<Entry> getNewEntries(@Nullable Date date, @Nullable String client) {
        List<Entry> entries = new ArrayList<>();
        String sql = "SELECT " + absoluteAllEntriesColumns + ", " + TABLE_CLIENTS + "." + CLIENTS_COLUMN_NAME +
                " FROM " + TABLE_ENTRIES + ", " + TABLE_CLIENTS +
                " WHERE " + TABLE_ENTRIES + "." + COLUMN_CLIENT_ID + " = " + TABLE_CLIENTS + "." + CLIENTS_COLUMN_ID;
        if (date != null) {
            long startTime = Util.getStartOfDay(date).getTime() / 1000;
            long endTime = Util.getEndOfDay(date).getTime() / 1000;
            sql += " AND " + COLUMN_DATE + " > " + startTime + " AND " + COLUMN_DATE + " < " + endTime;
        }
        if (client != null) {
            sql += " AND " + TABLE_CLIENTS + "." + CLIENTS_COLUMN_NAME + " = '" + client + "'";
        }
        Cursor cursor = database.rawQuery(sql, null);
        MySQLiteHelper.displayCursor(cursor, false);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Entry entry = newCursorToEntry(cursor, null);
            if (entry != null) {
                // check if the WHERE query works and if not it is to be replaced
                if (!Util.isSameDay(entry.date, date)) {
                    Log.e("Error Log by Dev :)", "Not same day!");
                    entry.work += " Wrong date! Supposed to be on " + Util.formatDate(date) + ", " +
                            "please inform developer!";
                }
                entries.add(entry);
            }
            cursor.moveToNext();
        }
        cursor.close();
        Collections.reverse(entries);
        return entries;
    }

    private List<Entry> getEntries(@Nullable Date date, @Nullable String client) {
        if (DATABASE_VERSION == THIRD_VERSION) {
            return getNewEntries(date, client);
        } else {
            List<Entry> entries = new ArrayList<>();
            String where = "";
            if (date != null) {
                long startTime = Util.getStartOfDay(date).getTime() / 1000;
                long endTime = Util.getEndOfDay(date).getTime() / 1000;
                where += COLUMN_DATE + " > " + startTime + " AND " + COLUMN_DATE + " < " + endTime;
            }
            if (client != null) {
                if (!where.equals("")) where += " AND ";
                where += COLUMN_CLIENT + " = '" + client + "'";
            }
            Cursor cursor = database.query(TABLE_ENTRIES, allEntriesColumns, where, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Entry entry = cursorToEntry(cursor, null);
                if (entry != null) {
                    // check if the WHERE query works and if not it is to be replaced
                    if (!Util.isSameDay(entry.date, date)) {
                        Log.e("Error Log by Dev :)", "Not same day!");
                        entry.work += " Wrong date! Supposed to be on " + Util.formatDate(date) + ", " +
                                "please inform developer!";
                    }
                    entries.add(entry);
                }
                cursor.moveToNext();
            }
            cursor.close();
            Collections.reverse(entries);
            return entries;
        }
    }

    public List<List<Entry>> getLastWeekEntries(Date date) {
        Date[] lastWeek = Util.getDatesOfLastWeek(date);
        List<List<Entry>> weekEntries = new ArrayList<>(4);
        for (Date day : lastWeek) {
            List<Entry> entries = getEntries(day);
            weekEntries.add(entries);
        }
        return weekEntries;
    }

    public static Entry newCursorToEntry(Cursor cursor, @Nullable Date onlyDate) {
        Entry entry = null;
        if(cursor.getCount() > 0) {
            int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
            String client = cursor.getString(cursor.getColumnIndex(CLIENTS_COLUMN_NAME));
            long time = (long) cursor.getInt(cursor.getColumnIndex(COLUMN_DATE)) * 1000;
            Date date = new Date(time);
            if (onlyDate != null && !Util.isSameDay(date, onlyDate)) return null;
            int duration = cursor.getInt(cursor.getColumnIndex(COLUMN_DURATION));
            int installerId = cursor.getInt(cursor.getColumnIndex(COLUMN_INSTALLER_ID));
            String work = cursor.getString(cursor.getColumnIndex(COLUMN_WORK));
            String picturePath = cursor.getString(cursor.getColumnIndex(COLUMN_PICTURE_PATH));

            entry = new Entry(
                    client,
                    date,
                    duration,
                    installerId,
                    work
            );
            entry.id = id;
            entry.setPicturePath(picturePath);
        }
        return entry;
    }

    public static Entry cursorToEntry(Cursor cursor, @Nullable Date onlyDate) {
        if(cursor.getCount() == 0) return null;
        int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
        String client = cursor.getString(cursor.getColumnIndex(COLUMN_CLIENT));
        long time = (long) cursor.getInt(cursor.getColumnIndex(COLUMN_DATE)) * 1000;
        Date date = new Date(time);
        if(onlyDate != null && !Util.isSameDay(date, onlyDate)) return null;
        int duration = cursor.getInt(cursor.getColumnIndex(COLUMN_DURATION));
        int installerId = cursor.getInt(cursor.getColumnIndex(COLUMN_INSTALLER_ID));
        String work = cursor.getString(cursor.getColumnIndex(COLUMN_WORK));
        String picturePath = cursor.getString(cursor.getColumnIndex(COLUMN_PICTURE_PATH));

        Entry entry = new Entry(
                client,
                date,
                duration,
                installerId,
                work
        );
        entry.id = id;
        entry.setPicturePath(picturePath);
        return entry;
    }

    private int addClient(String client) {
        ContentValues values = new ContentValues();
        values.put(CLIENTS_COLUMN_NAME, client);
        return (int) database.insertWithOnConflict(TABLE_CLIENTS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public int saveEntry(Entry entry) {
        if(entry.installerId == -1) throw new IllegalArgumentException("You cannot add an entry without an installer!");

        ContentValues values;
        if (DATABASE_VERSION == THIRD_VERSION) {
            int clientId = updateOrInsertClient(entry.client);
            values = entryToValues(entry, clientId);
        } else {
            values = entryToValues(entry);
        }

        // insert entry
        return (int) database.insert(TABLE_ENTRIES, null, values);
    }

    private int updateOrInsertClient(String client) {
        int clientId = -1;
        // check if client exists
        Cursor cursor = database.query(TABLE_CLIENTS, new String[]{CLIENTS_COLUMN_ID},
                CLIENTS_COLUMN_NAME + " = '" + client + "'",
                null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            clientId = cursor.getInt(cursor.getColumnIndex(CLIENTS_COLUMN_ID));
            cursor.moveToNext();
        }
        cursor.close();
        if (clientId == -1) clientId = addClient(client);
        return clientId;
    }

    public int addInstaller(String installer) {
        ContentValues values = new ContentValues();
        values.put(INSTALLERS_COLUMN_NAME, installer);
        return (int) database.insert(TABLE_INSTALLERS, null, values);
    }

    public BiMap<String, Integer> getInstallers() {
        BiMap<String, Integer> installers = HashBiMap.create();
        Cursor cursor = database.query(TABLE_INSTALLERS, allInstallersColumns, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int installerId = cursor.getInt(cursor.getColumnIndex(INSTALLERS_COLUMN_ID));
            String installer = cursor.getString(cursor.getColumnIndex(INSTALLERS_COLUMN_NAME));
            installers.put(installer, installerId);
            cursor.moveToNext();
        }
        cursor.close();
        return installers;
    }

    public List<Entry> idToInstaller(List<Entry> entries) {
        BiMap<Integer, String> biInstallers = getInstallers().inverse();

        for(Entry entry : entries) {
            entry.installer = biInstallers.get(entry.installerId);
        }
        return entries;
    }

    // TODO not very efficient
    public Entry idToInstaller(Entry entry) {
        List<Entry> entryList = new ArrayList<Entry>();
        entryList.add(entry);
        return idToInstaller(entryList).get(0);
    }

    public List<Entry> getEntriesWithInstaller(@Nullable Date date) {
        return idToInstaller(getEntries(date, null));
    }

    public List<Entry> getEntriesWithInstaller(@Nullable Date date, @Nullable String client) {
        return idToInstaller(getEntries(date, client));
    }

    public void editEntry(Entry entry) {
        if(entry.id != -1) {
            ContentValues values;
            if (DATABASE_VERSION == THIRD_VERSION) {
                int clientId = updateOrInsertClient(entry.client);
                values = entryToValues(entry, clientId);
            } else {
                values = entryToValues(entry);
            }
            database.update(TABLE_ENTRIES, values, COLUMN_ID + "=" + entry.id, null);
        }
    }

    private ContentValues entryToValues(Entry entry, int clientId) {
        ContentValues entryValues = new ContentValues();
        long time = entry.date.getTime() / 1000;
        entryValues.put(COLUMN_DATE, time);
        entryValues.put(COLUMN_CLIENT_ID, clientId);
        entryValues.put(COLUMN_DURATION, entry.duration);
        entryValues.put(COLUMN_INSTALLER_ID, entry.installerId);
        entryValues.put(COLUMN_WORK, entry.work);

        return entryValues;
    }

    private ContentValues entryToValues(Entry entry) {
        ContentValues entryValues = new ContentValues();
        long time = entry.date.getTime() / 1000;
        entryValues.put(COLUMN_DATE, time);
        entryValues.put(COLUMN_DURATION, entry.duration);
        entryValues.put(COLUMN_INSTALLER_ID, entry.installerId);
        entryValues.put(COLUMN_WORK, entry.work);

        return entryValues;
    }

    // return true if exactly one row was removed
    public boolean deleteEntry(Entry entry) {
        return database.delete(TABLE_ENTRIES, COLUMN_ID + "=" + entry.id, null) == 1;
    }

    public Entry loadAverageEntry() {
        if (DATABASE_VERSION == THIRD_VERSION) {
            String sql = "SELECT " + absoluteAllEntriesColumns + ", " + TABLE_CLIENTS + "." + CLIENTS_COLUMN_NAME +
                    " FROM " + TABLE_ENTRIES + ", " + TABLE_CLIENTS +
                    " WHERE " + TABLE_ENTRIES + "." + COLUMN_CLIENT_ID + " = " + TABLE_CLIENTS + "." + CLIENTS_COLUMN_ID;
            sql += " ORDER BY " + TABLE_ENTRIES + "." + COLUMN_DATE + " DESC LIMIT 1";
            Cursor cursor = database.rawQuery(sql, null);
            cursor.moveToFirst();
            Entry entry = (cursor.getCount() > 0) ? newCursorToEntry(cursor, null) : null;
            cursor.close();
            return entry;
        } else {
            // actually i'm just going to load the last entry
            Cursor cursor = database.query(
                    TABLE_ENTRIES, allEntriesColumns, null, null, null, null, COLUMN_DATE + " DESC", "1");
            cursor.moveToFirst();
            return (cursor.getCount() > 0) ? cursorToEntry(cursor, null) : null;
        }
    }

    public boolean deleteInstaller(int installerId) {
        boolean installerDeleted = database.delete(TABLE_INSTALLERS, INSTALLERS_COLUMN_ID + "=" + installerId, null) == 1;
        database.delete(TABLE_ENTRIES, COLUMN_INSTALLER_ID + "=" + installerId, null);
        return installerDeleted;
    }

    public void upgradeDurations() {
        int[] durations = {0, -1, 1, -1, 2, -1, 3, -1, 4, -1, 5, -1, 6, -1, 7, -1, 8, -1, 9, -1, 10, -1, 11, -1, 12, -1, -13, -1, 14, -1, 15, -1, 16, -1, 17, -1, 18, -1, 19, -1, 20, -1};
        for (int i = durations.length - 1; i > 0; i--) {
            //database.update(TABLE_ENTRIES, values, COLUMN_ID + "=" + entry.id, null);
            if(durations[i] != -1) database.execSQL("UPDATE " + TABLE_ENTRIES + " SET " + COLUMN_DURATION + " = " + i + " WHERE " + COLUMN_DURATION + " = " + durations[i]);
        }
        //database.execSQL("UPDATE " + TABLE_ENTRIES + " SET " + COLUMN_DURATION + " = " + "("+COLUMN_DURATION+"  * 2) - 1" + " WHERE " + COLUMN_DURATION + " > 2");
    }

    // TODO doesnt work
    public void renameInstaller(String oldInstaller, String newInstaller) {
        ContentValues values = new ContentValues();
        values.put(INSTALLERS_COLUMN_NAME, newInstaller);
        //return 1 == database.update(TABLE_INSTALLERS, values, INSTALLERS_COLUMN_NAME + " = " + oldInstaller, null);
        database.execSQL("UPDATE " + TABLE_INSTALLERS + " SET " + INSTALLERS_COLUMN_NAME + " = '" + newInstaller + "' WHERE " + INSTALLERS_COLUMN_NAME + " = '" + oldInstaller + "'");
    }

    public List<String> getNewAllClients(boolean trim) {
        List<String> clients = new ArrayList<>();
        Cursor cursor = database.rawQuery("SELECT * FROM " + TABLE_CLIENTS + " ORDER BY " + CLIENTS_COLUMN_NAME + " ASC", null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String client = cursor.getString(cursor.getColumnIndex(CLIENTS_COLUMN_NAME));
            if (trim) client = client.trim();
            clients.add(client);
            cursor.moveToNext();
        }
        cursor.close();
        // remove duplicates
        clients = new ArrayList<>(new LinkedHashSet<>(clients));
        return clients;
    }

    public List<String> getAllClients(boolean trim) {
        if (DATABASE_VERSION == THIRD_VERSION) {
            return getNewAllClients(trim);
        } else {
            List<String> clients = new ArrayList<>();
            Cursor cursor = database.query(true, TABLE_ENTRIES, new String[]{COLUMN_CLIENT}, null, null, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String client = cursor.getString(cursor.getColumnIndex(COLUMN_CLIENT));
                if (trim) client = client.trim();
                clients.add(client);
                cursor.moveToNext();
            }
            cursor.close();
            // remove duplicates
            clients = new ArrayList<>(new LinkedHashSet<>(clients));
            return clients;
        }
    }

    public int addPictureToEntry(int entryId, String pictureFile) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_PICTURE_PATH, pictureFile);
        return database.update(TABLE_ENTRIES, values, COLUMN_ID + " = " + entryId, null);
    }

    public void deletePictureFromEntry(int entryId) {
        ContentValues values = new ContentValues();
        values.putNull(COLUMN_PICTURE_PATH);
        database.update(TABLE_ENTRIES, values, COLUMN_ID + " = " + entryId, null);
    }

    public List<Entry> getEntriesForClient(String client) {
        return getEntriesWithInstaller(null, client);
    }
}
