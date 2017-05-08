package com.agold.sos.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by root on 17-4-19.
 */

public class NumberProvider {
    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "phoneName";
    public static final String KEY_NUM = "phoneNum";
    public static final String KEY_TYPE = "itemType";
    public static final String KEY_SAVE = "isSave";

    private final Context mContext;
    private SQLiteDatabase mSQLiteDataBase = null;
    private ContactDataBaseHelper mDataBaseHelper = null;

    public NumberProvider(Context context){
        mContext = context;
    }

    public void open() throws SQLException {
        mDataBaseHelper = ContactDataBaseHelper.getInstance(mContext);
        mSQLiteDataBase = mDataBaseHelper.getWritableDatabase();
    }

    public void close(){
        mDataBaseHelper.close();
    }

    public Long insertData(String name,String num,int save){
        return this.insertData(-1,name,num,save);
    }

    public Long insertData(int key,String name,String num,int save){
        android.util.Log.i("ly20170419","name --->"+name);
        android.util.Log.i("ly20170419","num --->"+num);
        long ret = 0;
        if(num == null || (null != null && num.isEmpty())){
            android.util.Log.i("ly20170419","fuck this is empty");
            return (long) mSQLiteDataBase.delete(ContactDataBaseHelper.NUMBER_TABLE, KEY_ID + "= " + key, null);
        }
        Cursor cursor = mSQLiteDataBase.query(true,
                ContactDataBaseHelper.NUMBER_TABLE,
                new String[]{KEY_ID,KEY_NAME,KEY_NUM,KEY_TYPE,KEY_SAVE},
                KEY_NUM + "= '" + num + "'",
                null,
                null,
                null,
                null,
                null);
        android.util.Log.i("ly20170419","now we have queryed");
        if(cursor != null && cursor.getCount() >0){
            this.deleteData(num);
            android.util.Log.i("ly20170419","delete data");
        }
        cursor.close();
        ContentValues values =  new ContentValues();
        values.put(KEY_NAME,name);
        values.put(KEY_NUM,num);
        values.put(KEY_SAVE,save);
        //values.put(KEY_ID,key);

        if(key >= 0 ){
            cursor = mSQLiteDataBase.query(true,
                    ContactDataBaseHelper.NUMBER_TABLE,
                    new String[]{KEY_ID,KEY_NAME,KEY_NUM,KEY_TYPE,KEY_SAVE},
                    KEY_ID + "= " + key,
                    null,
                    null,
                    null,
                    null,
                    null);
            if(cursor !=null && cursor.getCount() > 0){
                cursor.close();
                android.util.Log.i("ly20170419","return from 1");
                return (long) mSQLiteDataBase.update(ContactDataBaseHelper.NUMBER_TABLE,values,KEY_ID + "=" + key,null);
            }else{
                android.util.Log.i("ly20170419","return from 2");
                return mSQLiteDataBase.insert(ContactDataBaseHelper.NUMBER_TABLE,null,values);
            }
        }else{
            android.util.Log.i("ly20170419","return from 3");
            return mSQLiteDataBase.insert(ContactDataBaseHelper.NUMBER_TABLE,KEY_ID,values);
        }
    }

    public static boolean isPhoneNumberValid(String phoneNumber) {
        boolean isValid = false;
        String expression = "((^(13|15|18)[0-9]{9}$)|((^0[1,2]{1}\\d{1}(-?|\\s*))?\\d{4}\\s*\\d{3,4}$)|"
                + "((^0[3-9]{1}\\d{2}(-?|\\s*))?\\d{4}\\s*\\d{3,4}$)|"
                + "((^0[1,2]{1}\\d{1}(-?|\\s*))?\\d{4}\\s*\\d{3,4}['w','W','p','P',\\;,\\,](\\d{1,4})$)|"
                + "((^0[3-9]{1}\\d{2}(-?|\\s*))?\\d{4}\\s*\\d{3,4}['w','W','p','P',\\;,\\,](\\d{1,4})$))";
        CharSequence inputStr = phoneNumber;
        Pattern pattern = Pattern.compile(expression);
        Matcher matcher = pattern.matcher(inputStr);
        if (matcher.matches()) {
            isValid = true;
        }
        return isValid;
    }

    public Cursor query(){
        return mSQLiteDataBase.query(ContactDataBaseHelper.NUMBER_TABLE,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public Cursor fetchListData(String num) throws SQLException{
        Cursor cursor = mSQLiteDataBase.query(true,
                ContactDataBaseHelper.NUMBER_TABLE,
                new String[]{KEY_ID,KEY_NAME,KEY_NUM,KEY_TYPE,KEY_SAVE},
                KEY_NUM + "= '" + num +"'",
                null,
                null,
                null,
                null,
                null);
        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    public String queryNameByNumber(String num) throws SQLException{
        String name = null;
        Cursor cursor = mSQLiteDataBase.query(true,
                ContactDataBaseHelper.NUMBER_TABLE,
                new String[] {KEY_ID, KEY_NAME, KEY_NUM, KEY_TYPE, KEY_SAVE},
                KEY_NUM + "= '" + num + "'",
                null,
                null,
                null,
                null,
                null);
        if(cursor != null){
            cursor.moveToFirst();
            int curSize = cursor.getCount();
            try{
                if(curSize > 0){
                    final int nameIndex = cursor.getColumnIndexOrThrow(NumberProvider.KEY_NAME);
                    do{
                        name = cursor.getString(nameIndex);
                    }while(cursor.moveToFirst());
                }
            }finally {
                cursor.close();
            }
        }
        return name;
    }

    public int delete(Uri uri,String selection,String[] selectionArgs){
        mSQLiteDataBase.delete(ContactDataBaseHelper.NUMBER_TABLE,selection,selectionArgs);
        return 0;
    }

    public boolean deleteData(String num){
        return mSQLiteDataBase.delete(ContactDataBaseHelper.NUMBER_TABLE, KEY_NUM + "= '" + num + "'", null) > 0;
    }

    public boolean updateData(Long id, String num, int selected) {
        ContentValues values = new ContentValues();
        values.put(KEY_NUM, num);
        values.put(KEY_SAVE, selected);
        return mSQLiteDataBase.update(ContactDataBaseHelper.NUMBER_TABLE, values, KEY_ID + "=" + id, null) > 0;
    }

    public boolean isNumberExist(String num) {

        if (num == null) {
            return false;
        }

        Cursor cursor = mSQLiteDataBase.query(true,
                ContactDataBaseHelper.NUMBER_TABLE,
                new String[] {KEY_ID, KEY_NAME, KEY_NUM, KEY_TYPE, KEY_SAVE},
                KEY_NUM + "= '" + num + "'",
                null,
                null,
                null,
                null,
                null);
        if (cursor != null && cursor.getCount() == 0) {
            return true;
        } else {
            return false;
        }
    }
}
