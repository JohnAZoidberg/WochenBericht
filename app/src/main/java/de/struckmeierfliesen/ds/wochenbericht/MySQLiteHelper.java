package de.struckmeierfliesen.ds.wochenbericht;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class MySQLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_ENTRIES = "entries";
    public static final String COLUMN_ID = "id";
    @Deprecated public static final String COLUMN_CLIENT = "client";
    public static final String COLUMN_CLIENT_ID = "client_id";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_INSTALLER_ID = "installer_id";
    public static final String COLUMN_WORK = "work";
    public static final String COLUMN_PICTURE_PATH = "picture_path";

    public static final String TABLE_INSTALLERS = "installers";
    public static final String INSTALLERS_COLUMN_ID = "id";
    public static final String INSTALLERS_COLUMN_NAME = "name";

    public static final String TABLE_CLIENTS = "clients";
    public static final String CLIENTS_COLUMN_ID = "id";
    public static final String CLIENTS_COLUMN_NAME = "name";
    public static final String CLIENTS_COLUMN_TEL = "tel";
    public static final String CLIENTS_COLUMN_ADRESS = "adress";

    public static final String DATABASE_NAME = "shortcuts.db";
    public static final int FIRST_VERSION = 1;
    public static final int SECOND_VERSION = 2;
    public static final int THIRD_VERSION = 3;
    public static final int FOURTH_VERSION = 4;
    public static final int DATABASE_VERSION = FOURTH_VERSION;

    // Database creation sql statement
    private static final String CREATE_TABLE_ENTRIES = "CREATE TABLE IF NOT EXISTS "
            + TABLE_ENTRIES + "("
            + COLUMN_ID + " integer primary key, "
            + COLUMN_CLIENT + " text not null, "
            + COLUMN_DATE + " integer not null, "
            + COLUMN_DURATION + " integer not null, "
            + COLUMN_INSTALLER_ID + " integer not null, "
            + COLUMN_PICTURE_PATH + " text, "
            + COLUMN_WORK + " text not null);";

    private static final String CREATE_NEW_TABLE_ENTRIES = "CREATE TABLE IF NOT EXISTS "
            + TABLE_ENTRIES + "("
            + COLUMN_ID + " integer primary key, "
            //+ COLUMN_CLIENT + " text not null, "
            + COLUMN_CLIENT_ID + " integer not null, "
            + COLUMN_DATE + " integer not null, "
            + COLUMN_DURATION + " integer not null, "
            + COLUMN_INSTALLER_ID + " integer not null, "
            + COLUMN_PICTURE_PATH + " text, "
            + COLUMN_WORK + " text not null"
            + ", FOREIGN KEY (" + COLUMN_CLIENT_ID + ") REFERENCES " + TABLE_CLIENTS + "(" + CLIENTS_COLUMN_ID + ")"
            + ");";

    private static final String CREATE_TABLE_INSTALLERS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_INSTALLERS + "("
            + INSTALLERS_COLUMN_ID + " integer primary key, "
            + INSTALLERS_COLUMN_NAME + " text not null);";

    private static final String CREATE_TABLE_CLIENTS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_CLIENTS + "("
            + CLIENTS_COLUMN_ID + " integer primary key, "
            + CLIENTS_COLUMN_NAME + " text non null, "
            + CLIENTS_COLUMN_ADRESS + " text, "
            + CLIENTS_COLUMN_TEL + " text"
            + ");";

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_TABLE_ENTRIES);
        database.execSQL(CREATE_TABLE_INSTALLERS);
        database.execSQL(CREATE_TABLE_CLIENTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("onUpgrade", "OldVersion: " + oldVersion + " - newVersion: " + newVersion);
        final String TEMP_TABLE = "temp_table";
        switch (newVersion) {
            case SECOND_VERSION:
            String upgradeQuery = "ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_PICTURE_PATH + " TEXT";
            db.execSQL(upgradeQuery);

            case THIRD_VERSION:
            // Create new table
            db.execSQL(CREATE_TABLE_CLIENTS);

            // Fetch all existing clients
            List<String> clients = new ArrayList<>();
            String[] cols = {COLUMN_CLIENT};
            Cursor cursor = db.query(TABLE_ENTRIES, cols, null, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                clients.add(cursor.getString(cursor.getColumnIndex(COLUMN_CLIENT)));
                cursor.moveToNext();
            }
            cursor.close();
            // remove duplicates
            clients = new ArrayList<>(new LinkedHashSet<>(clients));

            // add new row
            db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_CLIENT_ID + " integer");

            // insert ids into old table
            for (int i = 0; i < clients.size(); i++) {
                String client = clients.get(i);
                ContentValues values = new ContentValues();
                values.put(COLUMN_CLIENT_ID, i + 1);
                int update = db.update(TABLE_ENTRIES, values, COLUMN_CLIENT + " = '" + client + "'", null);

                values = new ContentValues();
                values.put(CLIENTS_COLUMN_NAME, client);
                int insert = (int) db.insert(TABLE_CLIENTS, null, values);
            }

            // SFSDJFLKSDFJLSDFJLSKDJFKLÖSJFKLÖJSDFKLÖJSDFLÖJSDÖFKLJSDÖFKLJSDFÖLJSFLÖKD
            // rename table
            db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " RENAME TO " + TEMP_TABLE);

            // create new table
            db.execSQL(CREATE_NEW_TABLE_ENTRIES);

            // fill new table
            db.execSQL("INSERT OR IGNORE INTO " + TABLE_ENTRIES + " ("
                    + COLUMN_ID + ", "
                    + COLUMN_CLIENT_ID + ", "
                    + COLUMN_DATE + ", "
                    + COLUMN_DURATION + ", "
                    + COLUMN_INSTALLER_ID + ", "
                    + COLUMN_PICTURE_PATH + ", "
                    + COLUMN_WORK
                    + ") SELECT "
                    + COLUMN_ID + ", "
                    + COLUMN_CLIENT_ID + ", "
                    + COLUMN_DATE + ", "
                    + COLUMN_DURATION + ", "
                    + COLUMN_INSTALLER_ID + ", "
                    + COLUMN_PICTURE_PATH + ", "
                    + COLUMN_WORK
                    + " FROM " + TEMP_TABLE);

            // remove old table
            db.execSQL("DROP TABLE " + TEMP_TABLE);

            // KLJSDFKLÖJSDKLÖFJ KLSDJF KLJSDFKL ÖJSKLÖF JSLÖKDJFKLÖSDJFLÖ KJSDF

            String sql = "SELECT " + TABLE_ENTRIES + "." + COLUMN_WORK + ", " + TABLE_CLIENTS + "." + CLIENTS_COLUMN_NAME +
                    " FROM " + TABLE_ENTRIES + ", " + TABLE_CLIENTS +
                    " WHERE " + TABLE_ENTRIES + "." + COLUMN_CLIENT_ID + " = " + TABLE_CLIENTS + "." + CLIENTS_COLUMN_ID;
            cursor = db.rawQuery(
                    sql + " AND " + TABLE_ENTRIES + "." + COLUMN_WORK + " = 'Wartung '", null);
            displayCursor(cursor, false);

            case FOURTH_VERSION:
                // rename table
                db.execSQL("ALTER TABLE " + TABLE_CLIENTS + " RENAME TO " + TEMP_TABLE);

                // create new table
                db.execSQL(CREATE_TABLE_CLIENTS);

                // fill new table
                db.execSQL("INSERT OR IGNORE INTO " + TABLE_CLIENTS + " SELECT * FROM " + TEMP_TABLE);

                // remove old table
                db.execSQL("DROP TABLE " + TEMP_TABLE);
                Log.d("fasdf", "onUpgrade: jasdlkfjasklfj");

        }
    }

    public static void displayCursor(Cursor cursor, boolean close) {
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int columnCount = cursor.getColumnCount();
            String row = "";
            for (int i = 0; i < columnCount; i++) {
                if (!row.equals("")) row += ", ";
                row += cursor.getColumnName(i) + ": " + cursor.getString(i);
            }
            Log.d("DisplayCursor", row);
            cursor.moveToNext();
        }
        if (close) cursor.close();
    }
}