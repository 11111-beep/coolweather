package com.example.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.example.coolweather.gson.Weather;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Callback;

import okhttp3.Call;
import okhttp3.Response;

public class AutoUpdateService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        // 由于该服务不需要与其他组件绑定，返回null
        return null;
    }

    /**
     * Called by the system when the service is first created
     * 负责启动服务并执行定时任务
     *
     * @param intent  Intent: 启动服务的意图
     * @param flags  int: 标志，用于指定服务的运行模式
     * @param startId int: 唯一的ID，用于标识服务的启动请求
     * @return int: 表示服务的运行模式
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 更新天气数据
        updateWeather();
        // 更新Bing每日图片
        updateBingPic();

        // 获取AlarmManager实例，用于设置定时任务
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // 设置8小时的间隔（单位：毫秒）
        int anHour = 8 * 60 * 60 * 1000; // 8小时
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour; // 计算触发时间

        // 创建Intent，用于启动服务
        Intent i = new Intent(this, AutoUpdateService.class);
        // 创建PendingIntent，用于在指定时间执行服务
        PendingIntent pi = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_IMMUTABLE);

        // 取消之前的闹钟任务（如果有）
        manager.cancel(pi);
        // 设置新的闹钟任务
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);

        // 返回super类的onStartCommand的返回值
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 更新天气数据
     * 从SharedPreferences中获取缓存的天气数据，如果有缓存，则更新最新的天气信息
     */
    private void updateWeather() {
        // 获取SharedPreferences的实例，用于存取数据
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // 获取缓存的天气数据
        String weatherString = prefs.getString("weather", null);

        if (weatherString != null) {
            // 如果有缓存数据，解析天气信息
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;

            // 构造获取天气的URL
            String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";

            // 发送HTTP请求获取最新天气数据
            HttpUtil.sendHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(responseText);

                    if (weather != null && "ok".equals(weather.status)) {
                        // 更新SharedPreferences，保存最新的天气数据
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    // 处理网络请求失败的情况
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * 更新Bing每日图片URL
     * 从服务器获取最新的Bing图片URL，并保存到SharedPreferences中
     */
    private void updateBingPic() {
        // 请求Bing图片的URL
        String requestBingPic = "http://guolin.tech/api/bing_pic";

        // 发送HTTP请求获取Bing图片URL
        HttpUtil.sendHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bingPic = response.body().string();

                // 保存获取到的Bing图片URL到SharedPreferences
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                // 处理网络请求失败的情况
                e.printStackTrace();
            }
        });
    }
}
