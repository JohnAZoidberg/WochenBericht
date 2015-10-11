package de.struckmeierfliesen.ds.wochenbericht;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

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

    private ArrayList<Entry> getEntries(Date date) {
        ArrayList<Entry> entries = new ArrayList<>();
        // TODO maybe check if update is necessary
        Cursor cursor = database.query(MySQLiteHelper.TABLE_ENTRIES, allEntriesColumns, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Entry entry = cursorToEntry(cursor, date);
            if(entry != null) entries.add(entry);
            cursor.moveToNext();
        }
        Collections.reverse(entries);
        return entries;
    }

    private Entry cursorToEntry(Cursor cursor, Date onlyDate) {
        if(cursor.getCount() == 0) return null;
        int id = cursor.getInt(cursor.getColumnIndex(MySQLiteHelper.COLUMN_ID));
        String client = cursor.getString(cursor.getColumnIndex(MySQLiteHelper.COLUMN_CLIENT));
        long time = (long) cursor.getInt(cursor.getColumnIndex(MySQLiteHelper.COLUMN_DATE)) * 1000;
        Date date = new Date(time);
        if(!Util.isSameDay(date, onlyDate)) return null;
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
        BiMap<Integer, String> biIstallers = getInstallers().inverse();

        for(Entry entry : entries) {
            entry.installer = biIstallers.get(entry.installerId);
        }
        return entries;
    }

    public ArrayList<Entry> getEntriesWithInstaller(Date date) {
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
}
