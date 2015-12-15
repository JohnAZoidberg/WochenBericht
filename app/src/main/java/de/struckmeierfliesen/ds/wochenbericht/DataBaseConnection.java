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
            COLUMN_CLIENT_ID,
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
        Log.d("JSON-Export: ", resultSet.toString());
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
        return getEntries(date, -1);
    }

    private List<Entry> getEntries(@Nullable Date date, int clientId) {
        List<Entry> entries = new ArrayList<>();
        String sql = "SELECT " + absoluteAllEntriesColumns + ", " + TABLE_CLIENTS + "." + CLIENTS_COLUMN_NAME +
                " FROM " + TABLE_ENTRIES + ", " + TABLE_CLIENTS +
                " WHERE " + TABLE_ENTRIES + "." + COLUMN_CLIENT_ID + " = " + TABLE_CLIENTS + "." + CLIENTS_COLUMN_ID;
        if (date != null) {
            long startTime = Util.getStartOfDay(date).getTime() / 1000;
            long endTime = Util.getEndOfDay(date).getTime() / 1000;
            sql += " AND " + COLUMN_DATE + " > " + startTime + " AND " + COLUMN_DATE + " < " + endTime;
        }
        if (clientId != -1) {
            sql += " AND " + TABLE_CLIENTS + "." + CLIENTS_COLUMN_ID + " = " + clientId + "";
        }
        Cursor cursor = database.rawQuery(sql, null);
        MySQLiteHelper.displayCursor(cursor, false);
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

    public List<List<Entry>> getLastWeekEntries(Date date) {
        Date[] lastWeek = Util.getDatesOfLastWeek(date);
        List<List<Entry>> weekEntries = new ArrayList<>(4);
        for (Date day : lastWeek) {
            List<Entry> entries = getEntries(day);
            weekEntries.add(entries);
        }
        return weekEntries;
    }

    public static Entry cursorToEntry(Cursor cursor, @Nullable Date onlyDate) {
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

    private int addClient(String client) {
        ContentValues values = new ContentValues();
        values.put(CLIENTS_COLUMN_NAME, client);
        return (int) database.insertWithOnConflict(TABLE_CLIENTS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public int saveEntry(Entry entry) {
        if(entry.installerId == -1) throw new IllegalArgumentException("You cannot add an entry without an installer!");

        int clientId = updateOrInsertClient(entry.client);
        ContentValues values = entryToValues(entry, clientId);

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
        return idToInstaller(getEntries(date, -1));
    }

    public List<Entry> getEntriesWithInstaller(@Nullable Date date, int clientId) {
        return idToInstaller(getEntries(date, clientId));
    }

    public void editEntry(Entry entry) {
        if(entry.id != -1) {
            int clientId = updateOrInsertClient(entry.client);
            ContentValues values = entryToValues(entry, clientId);
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

    // return true if exactly one row was removed
    public boolean deleteEntry(Entry entry) {
        return database.delete(TABLE_ENTRIES, COLUMN_ID + "=" + entry.id, null) == 1;
    }

    public Entry loadAverageEntry() {
        String sql = "SELECT " + absoluteAllEntriesColumns + ", " + TABLE_CLIENTS + "." + CLIENTS_COLUMN_NAME +
                " FROM " + TABLE_ENTRIES + ", " + TABLE_CLIENTS +
                " WHERE " + TABLE_ENTRIES + "." + COLUMN_CLIENT_ID + " = " + TABLE_CLIENTS + "." + CLIENTS_COLUMN_ID;
        sql += " ORDER BY " + TABLE_ENTRIES + "." + COLUMN_DATE + " DESC LIMIT 1";
        Cursor cursor = database.rawQuery(sql, null);
        cursor.moveToFirst();
        Entry entry = (cursor.getCount() > 0) ? cursorToEntry(cursor, null) : null;
        cursor.close();
        return entry;
    }

    public boolean deleteInstaller(int installerId) {
        boolean installerDeleted = database.delete(TABLE_INSTALLERS, INSTALLERS_COLUMN_ID + "=" + installerId, null) == 1;
        database.delete(TABLE_ENTRIES, COLUMN_INSTALLER_ID + "=" + installerId, null);
        return installerDeleted;
    }

    // TODO doesnt work
    public void renameInstaller(String oldInstaller, String newInstaller) {
        ContentValues values = new ContentValues();
        values.put(INSTALLERS_COLUMN_NAME, newInstaller);
        //return 1 == database.update(TABLE_INSTALLERS, values, INSTALLERS_COLUMN_NAME + " = " + oldInstaller, null);
        database.execSQL("UPDATE " + TABLE_INSTALLERS + " SET " + INSTALLERS_COLUMN_NAME + " = '" + newInstaller + "' WHERE " + INSTALLERS_COLUMN_NAME + " = '" + oldInstaller + "'");
    }

    public List<Client> getAllClientObjects(boolean trim) {
        List<Client> clients = new ArrayList<>();
        Cursor cursor = database.rawQuery("SELECT * FROM " + TABLE_CLIENTS + " ORDER BY " + CLIENTS_COLUMN_NAME + " ASC", null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int id = cursor.getInt(cursor.getColumnIndex(CLIENTS_COLUMN_ID));
            String name = cursor.getString(cursor.getColumnIndex(CLIENTS_COLUMN_NAME));
            if (trim) name = name.trim();
            Client client = new Client(id, name);
            client.tel = cursor.getString(cursor.getColumnIndex(CLIENTS_COLUMN_TEL));
            client.adress = cursor.getString(cursor.getColumnIndex(CLIENTS_COLUMN_ADRESS));
            clients.add(client);
            cursor.moveToNext();
        }
        cursor.close();
        // remove duplicates
        clients = new ArrayList<>(new LinkedHashSet<>(clients));
        return clients;
    }

    public List<String> getAllClients(boolean trim) {
        List<String> clients = new ArrayList<>();
        Cursor cursor = database.rawQuery("SELECT " + CLIENTS_COLUMN_NAME + " FROM " + TABLE_CLIENTS + " ORDER BY " + CLIENTS_COLUMN_NAME + " ASC", null);
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

    public List<Entry> getEntriesForClient(int clientId) {
        return getEntriesWithInstaller(null, clientId);
    }

    public void saveClientDetails(int clientId, String tel, String adress) {
        ContentValues values = new ContentValues();
        values.put(CLIENTS_COLUMN_TEL, tel);
        values.put(CLIENTS_COLUMN_ADRESS, adress);
        database.update(TABLE_CLIENTS, values, CLIENTS_COLUMN_ID + " = " + clientId, null);
    }

    private Client queryClient(String where) {
        Cursor cursor = database.rawQuery("SELECT * FROM " + TABLE_CLIENTS +
                        " WHERE " + where,
                null);
        cursor.moveToFirst();
        int clientId = cursor.getInt(cursor.getColumnIndex(CLIENTS_COLUMN_ID));
        String name = cursor.getString(cursor.getColumnIndex(CLIENTS_COLUMN_NAME));
        String adress = cursor.getString(cursor.getColumnIndex(CLIENTS_COLUMN_ADRESS));
        boolean noTel = cursor.isNull(cursor.getColumnIndex(CLIENTS_COLUMN_TEL));
        String tel = noTel ? null : cursor.getString(cursor.getColumnIndex(CLIENTS_COLUMN_TEL));
        cursor.close();
        Client client = new Client(clientId, name);
        client.adress = adress;
        client.tel = tel;
        return client;
    }


    public Client getClient(String client) {
        return queryClient(CLIENTS_COLUMN_NAME + " = '" + client + "'");
    }

    public Client getClient(int clientId) {
        return queryClient(CLIENTS_COLUMN_ID + " = " + clientId);
    }

    public void deleteClient(int clientId) {
        database.delete(TABLE_CLIENTS, CLIENTS_COLUMN_ID + " = " + clientId, null);
        database.delete(TABLE_ENTRIES, COLUMN_CLIENT_ID + " = " + clientId, null);
    }

    // -1 signals successful rename process, otherwise the id of the client with the same name is returned
    public int renameClient(int clientId, String newName) {
        Cursor query = database.query(TABLE_CLIENTS, new String[]{CLIENTS_COLUMN_ID}, CLIENTS_COLUMN_NAME + " = '" + newName + "'", null, null, null, null);
        //Cursor query = database.query(TABLE_ENTRIES, new String[]{COLUMN_ID}, COLUMN_CLIENT_ID + " = " + clientId, null, null, null, null);
        if (query.getCount() > 0) {
            query.moveToFirst();
            int oldId =  query.getInt(query.getColumnIndex(CLIENTS_COLUMN_ID));
            query.close();
            return oldId;
        } else {
            ContentValues values = new ContentValues();
            values.put(CLIENTS_COLUMN_NAME, newName);
            database.update(TABLE_CLIENTS, values, CLIENTS_COLUMN_ID + " = " + clientId, null);
        }
        return -1;
    }

    // merge is kept and with goes
    public void mergeClients(int merge, int with) {
        // edit old columns
        ContentValues values = new ContentValues();
        values.put(COLUMN_CLIENT_ID, merge);
        database.update(TABLE_ENTRIES, values, COLUMN_CLIENT_ID + " = " + with, null);

        // delete old client
        database.delete(TABLE_CLIENTS, CLIENTS_COLUMN_ID + " = " + with, null);
    }
}
