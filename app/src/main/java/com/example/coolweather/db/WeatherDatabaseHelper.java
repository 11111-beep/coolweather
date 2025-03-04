package com.example.coolweather.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class WeatherDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "cool_weather";
    public static final int DATABASE_VERSION = 1;

    public static final String CREATE_PROVINCE = "CREATE TABLE Province (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "province_name TEXT, " +
            "province_code INTEGER)";

    public static final String CREATE_CITY = "CREATE TABLE City (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "city_name TEXT, " +
            "city_code INTEGER, " +
            "province_id INTEGER, " +
            "FOREIGN KEY(province_id) REFERENCES Province(id))";

    public static final String CREATE_COUNTY = "CREATE TABLE County (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "county_name TEXT, " +
            "county_code INTEGER, " +
            "city_id INTEGER, " +
            "FOREIGN KEY(city_id) REFERENCES City(id))";

    public WeatherDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_PROVINCE);
        db.execSQL(CREATE_CITY);
        db.execSQL(CREATE_COUNTY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 升级数据库时先删除旧表再重新创建
        db.execSQL("DROP TABLE IF EXISTS Province");
        db.execSQL("DROP TABLE IF EXISTS City");
        db.execSQL("DROP TABLE IF EXISTS County");
        onCreate(db);
    }
}
