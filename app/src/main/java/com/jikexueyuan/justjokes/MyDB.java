package com.jikexueyuan.justjokes;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by fangc on 2016/2/5.
 */
public class MyDB extends SQLiteOpenHelper {
    public MyDB(Context context) {
        super(context, "db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String message = "create table if not exists cache("
                + "_id integer primary key autoincrement,"
                + "post_title TEXT DEFAULT \"\","
                + "post_content TEXT DEFAULT \"\","
                + "ID int DEFAULT \"\","
                + "page int DEFAULT \"\","
                + "post_date TEXT)";

        db.execSQL(message);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
