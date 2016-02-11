package com.reuniware.apps.mysmsbackup2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Utilisateur001 on 04/12/2015.
 */
public class MySmsBackupDbHelper extends SQLiteOpenHelper {

    String TAG = "MySmsBackupDbHelper";

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "mysmsbackup.db";

    public MySmsBackupDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "constructor");
    }

    private static final String sqlCreate = "create table credentials (login text, password text)";
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate");
        db.execSQL(sqlCreate);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade");
        // This database is only a cache for online data, so its upgrade policy is to simply to discard the data and start over
        db.execSQL(sqlCreate);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onDowngrade");
        onUpgrade(db, oldVersion, newVersion);
    }

    public void dropTables() {
        SQLiteDatabase dbw = getWritableDatabase();
        dbw.execSQL("drop table if exists credentials", new String[]{});
        dbw.close();
    }

    public void deleteAllCredentials(){
        SQLiteDatabase dbw = getWritableDatabase();
        //dbw.execSQL("delete from accesspoint", new String[]{});
        dbw.delete("credentials", "", new String[]{});
        dbw.close();
    }

    public void insertCredentials(String login, String password) {
        SQLiteDatabase dbw = getWritableDatabase();
        dbw.execSQL("insert into credentials (login, password) values (?,?)", new String[]{login, password});
        dbw.close();
    }

    public void updateCredentialsPassword(String login, String password) {
        SQLiteDatabase dbw = getWritableDatabase();
        dbw.execSQL("update credentials set password = ? where login = ?", new String[]{password, login});
        dbw.close();
    }

    public String getPasswordForLogin(String login){
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("select * from credentials where login = ?", new String[]{login});
        Log.d(TAG,"nb rec in db = " + c.getCount());
        String password = "";
        if (c.getCount()>0){
            c.moveToNext();
            //Log.d(TAG, "DB:" + c.getString(0) + " " + c.getString(1));
            password = c.getString(1);
        }
        c.close();
        db.close();
        return password;
    }

    public void getAllCredentials() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("select * from credentials", new String[]{});
        Log.d(TAG,"nb rec in db = " + c.getCount());
        for(int i=0;i<c.getCount();i++) {
            c.moveToNext();
            Log.d(TAG, "DB:" + c.getString(0) + " " + c.getString(1));
        }
        c.close();
        db.close();
    }

    public int getCredentialsCount(){
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("select * from credentials", new String[]{});
        int nb = c.getCount();
        c.close();
        return nb;
    }

    public boolean loginExistsInCredentials(String login){
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("select * from credentials where login=?", new String[]{login});
        if (c.getCount()>0){
            c.close();
            db.close();
            return true;
        }
        else {
            c.close();
            db.close();
            return false;
        }
    }


}
