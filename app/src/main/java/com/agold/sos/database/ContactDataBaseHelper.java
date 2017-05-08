package com.agold.sos.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by root on 17-4-19.
 */

public class ContactDataBaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sos.db";
    private static final int Version = 1;
    public static final String NUMBER_TABLE = "number";
    private static ContactDataBaseHelper SingleInstance = null;

    public static synchronized ContactDataBaseHelper getInstance(Context context){
        if(SingleInstance == null){
            SingleInstance = new ContactDataBaseHelper(context);
        }
        return SingleInstance;
    }

    public ContactDataBaseHelper(Context context){
        super(context,DATABASE_NAME,null,Version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + NUMBER_TABLE + " ("
        + "_id INTEGER PRIMARY KEY,"
        + "phoneName TEXT,"
        + "phoneNum TEXT,"
        + "itemType INTEGER,"
        + "isSave INTEGER"
        + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS" + NUMBER_TABLE + ";");
        onCreate(db);
    }
}
