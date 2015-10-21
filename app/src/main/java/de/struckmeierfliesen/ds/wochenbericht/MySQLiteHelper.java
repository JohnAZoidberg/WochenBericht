package de.struckmeierfliesen.ds.wochenbericht;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_ENTRIES = "entries";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CLIENT = "client";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_INSTALLER_ID = "installer_id";
    public static final String COLUMN_WORK = "work";

    public static final String TABLE_INSTALLERS = "installers";
    public static final String INSTALLERS_COLUMN_ID = "id";
    public static final String INSTALLERS_COLUMN_NAME = "name";

    public static final String DATABASE_NAME = "shortcuts.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String CREATE_TABLE_ENTRIES = "CREATE TABLE "
            + TABLE_ENTRIES + "("
            + COLUMN_ID + " integer primary key, "
            + COLUMN_CLIENT + " text not null, "
            + COLUMN_DATE + " integer not null, "
            + COLUMN_DURATION + " integer not null, "
            + COLUMN_INSTALLER_ID + " integer not null, "
            + COLUMN_WORK + " text not null);";

    private static final String CREATE_TABLE_INSTALLERS = "CREATE TABLE "
            + TABLE_INSTALLERS + "("
            + INSTALLERS_COLUMN_ID + " integer primary key, "
            + INSTALLERS_COLUMN_NAME + " text not null);";

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_TABLE_ENTRIES);
        database.execSQL(CREATE_TABLE_INSTALLERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENTRIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INSTALLERS);
        onCreate(db);
    }
}