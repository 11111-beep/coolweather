package com.example.coolweather.util;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public class Utility {
    public static boolean handleProvinceResponse(String response, SQLiteDatabase db) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allProvinces = new JSONArray(response);
                for (int i = 0; i < allProvinces.length(); i++) {
                    JSONObject provinceObject = allProvinces.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    values.put("province_name", provinceObject.getString("name"));
                    values.put("province_code", provinceObject.getInt("id"));
                    db.insert("Province", null, values);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean handleCityResponse(String response, SQLiteDatabase db, int provinceId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCities = new JSONArray(response);
                for (int i = 0; i < allCities.length(); i++) {
                    JSONObject cityObject = allCities.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    values.put("city_name", cityObject.getString("name"));
                    values.put("city_code", cityObject.getInt("id"));
                    values.put("province_id", provinceId);
                    db.insert("City", null, values);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean handleCountyResponse(String response, SQLiteDatabase db, int cityId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCounties = new JSONArray(response);
                for (int i = 0; i < allCounties.length(); i++) {
                    JSONObject countyObject = allCounties.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    values.put("county_name", countyObject.getString("name"));
                    values.put("county_code", countyObject.getInt("id"));
                    values.put("city_id", cityId);
                    db.insert("County", null, values);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
