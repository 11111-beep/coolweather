package com.example.coolweather;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.coolweather.db.WeatherDatabaseHelper;
import com.example.coolweather.db.Province;
import com.example.coolweather.db.City;
import com.example.coolweather.db.County;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    private static final String TAG = "AreaFragment"; // 日志标签
    public static final int LEVEL_PROVINCE = 0;    // 省级别
    public static final int LEVEL_CITY = 1;        // 市级别
    public static final int LEVEL_COUNTY = 2;      // 县级别

    private ProgressDialog progressDialog; // 加载进度对话框
    private TextView titleText;             // 标题文本
    private Button backButton;              // 后按钮
    private ListView listView;              // 列表视图
    private ArrayAdapter<String> adapter;   // 列表适配器
    private List<String> dataList = new ArrayList<>(); // 数据列表

    private List<Province> provinceList;    // 省份列表
    private List<City> cityList;            // 城市列表
    private List<County> countyList;        // 县区列表
    private Province selectedProvince;      // 选中的省份
    private City selectedCity;              // 选中的城市
    private int currentLevel;               // 当前级别

    private WeatherDatabaseHelper dbHelper; // 天气数据库帮助类

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 创建Fragment的视图
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = view.findViewById(R.id.title_text); // 初始化标题文本
        backButton = view.findViewById(R.id.back_button); // 初始化后按钮
        listView = view.findViewById(R.id.list_view); // 初始化列表视图
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList); // 创建适配器
        listView.setAdapter(adapter); // 设置适配器
        dbHelper = new WeatherDatabaseHelper(getContext()); // 初始化数据库帮助类
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // 设置列表项点击事件监听器
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position); // 获取选中的省份
                    queryCities(); // 查询该省份下的城市
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position); // 获取选中的城市
                    queryCounties(); // 查询该城市下的县区
                } else if (currentLevel == LEVEL_COUNTY) {
                    String weatherId = countyList.get(position).getWeatherId(); // 获取选中的县区的天气ID
                    if (getActivity() instanceof MainActivity) {
                        // 如果当前Activity是MainActivity，启动WeatherActivity
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId); // 传递天气ID
                        startActivity(intent);
                        getActivity().finish(); // 结束当前Activity
                    } else if (getActivity() instanceof WeatherActivity) {
                        // 如果当前Activity是WeatherActivity，直接请求天气数据
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers(); // 关闭抽屉
                        activity.swipeRefresh.setRefreshing(true); // 显示刷新指示器
                        activity.requestWeather(weatherId); // 请求天气数据
                    }
                }
            }
        });

        // 设置后按钮点击事件监听器
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities(); // 返回到城市列表
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces(); // 返回到省份列表
                }
            }
        });

        queryProvinces(); // 初始化时查询省份列表
    }

    /**
     * 查询省份列表
     * 从本地数据库查询，如果没有数据则从服务器获取
     */
    @SuppressLint("Range")
    private void queryProvinces() {
        titleText.setText("中国"); // 设置标题为"中国"
        backButton.setVisibility(View.GONE); // 隐藏后按钮
        provinceList = new ArrayList<>(); // 初始化省份列表
        dataList.clear(); // 清空数据列表

        SQLiteDatabase db = dbHelper.getReadableDatabase(); // 获取可读数据库
        Cursor cursor = db.rawQuery("SELECT * FROM Province", null); // 查询Province表
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Province province = new Province(); // 创建Province对象
                province.setId(cursor.getInt(cursor.getColumnIndex("id"))); // 设置ID
                province.setProvinceName(cursor.getString(cursor.getColumnIndex("province_name"))); // 设置省份名称
                province.setProvinceCode(cursor.getInt(cursor.getColumnIndex("province_code"))); // 设置省份代码
                provinceList.add(province); // 添加到省份列表
                dataList.add(province.getProvinceName()); // 添加到数据列表
            }
            cursor.close(); // 关闭游标
        }
        db.close(); // 关闭数据库

        if (provinceList.size() > 0) {
            adapter.notifyDataSetChanged(); // 通知适配器数据变化
            listView.setSelection(0); // 设置列表选中项为第一个
            currentLevel = LEVEL_PROVINCE; // 设置当前级别为省份
        } else {
            // 如果本地没有数据，从服务器获取
            String address = "http://guolin.tech/api/china"; // 服务器地址
            queryFromServer(address, "province"); // 从服务器查询省份
        }
    }

    /**
     * 查询城市列表
     * 从本地数据库查询，如果没有数据则从服务器获取
     */
    @SuppressLint("Range")
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName()); // 设置标题为选中的省份名称
        backButton.setVisibility(View.VISIBLE); // 显示后按钮
        cityList = new ArrayList<>(); // 初始化城市列表
        dataList.clear(); // 清空数据列表

        SQLiteDatabase db = dbHelper.getReadableDatabase(); // 获取可读数据库
        // 查询City表中province_id等于选中省份ID的记录
        Cursor cursor = db.rawQuery("SELECT * FROM City WHERE province_id = ?", new String[]{String.valueOf(selectedProvince.getId())});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                City city = new City(); // 创建City对象
                city.setId(cursor.getInt(cursor.getColumnIndex("id"))); // 设置ID
                city.setCityName(cursor.getString(cursor.getColumnIndex("city_name"))); // 设置城市名称
                city.setCityCode(cursor.getInt(cursor.getColumnIndex("city_code"))); // 设置城市代码

                cityList.add(city); // 添加到城市列表
                dataList.add(city.getCityName()); // 添加到数据列表
            }
            cursor.close(); // 关闭游标
        }
        db.close(); // 关闭数据库

        if (cityList.size() > 0) {
            adapter.notifyDataSetChanged(); // 通知适配器数据变化
            listView.setSelection(0); // 设置列表选中项为第一个
            currentLevel = LEVEL_CITY; // 设置当前级别为城市
        } else {
            // 如果本地没有数据，从服务器获取
            String provinceCode = String.valueOf(selectedProvince.getProvinceCode()); // 获取省份代码
            String address = "http://guolin.tech/api/china/" + provinceCode; // 服务器地址
            queryFromServer(address, "city"); // 从服务器查询城市
        }
    }

    /**
     * 查询县区列表
     * 从本地数据库查询，如果没有数据则从服务器获取
     */
    @SuppressLint("Range")
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName()); // 设置标题为选中的城市名称
        backButton.setVisibility(View.VISIBLE); // 显示后按钮
        countyList = new ArrayList<>(); // 初始化县区列表
        dataList.clear(); // 清空数据列表

        SQLiteDatabase db = dbHelper.getReadableDatabase(); // 获取可读数据库
        // 查询County表中city_id等于选中城市ID的记录
        Cursor cursor = db.rawQuery("SELECT * FROM County WHERE city_id = ?", new String[]{String.valueOf(selectedCity.getId())});
        Log.d(TAG, "Cursor column count: " + cursor.getColumnCount()); // 打印日志，调试用
        if (cursor != null) {
            while (cursor.moveToNext()) {
                County county = new County(); // 创建County对象
                county.setId(cursor.getInt(cursor.getColumnIndex("id"))); // 设置ID
                county.setCountyName(cursor.getString(cursor.getColumnIndex("county_name"))); // 设置县区名称
                county.setWeatherId(cursor.getString(cursor.getColumnIndex("weather_id"))); // 设置天气ID

                countyList.add(county); // 添加到县区列表
                dataList.add(county.getCountyName()); // 添加到数据列表
            }
            cursor.close(); // 关闭游标
        }
        db.close(); // 关闭数据库

        if (countyList.size() > 0) {
            adapter.notifyDataSetChanged(); // 通知适配器数据变化
            listView.setSelection(0); // 设置列表选中项为第一个
            currentLevel = LEVEL_COUNTY; // 设置当前级别为县区
        } else {
            // 如果本地没有数据，从服务器获取
            String provinceCode = String.valueOf(selectedProvince.getProvinceCode()); // 获取省份代码
            String cityCode = String.valueOf(selectedCity.getCityCode()); // 获取城市代码
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode; // 服务器地址
            queryFromServer(address, "county"); // 从服务器查询县区
        }
    }

    /**
     * 从服务器查询数据
     * @param address 服务器地址
     * @param type   查询类型：province、city、county
     */
    private void queryFromServer(String address, final String type) {
        showProgressDialog(); // 显示加载进度对话框

        // 发送HTTP请求
        HttpUtil.sendHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string(); // 获取响应正文
                boolean result = false; // 是否处理成功
                SQLiteDatabase db = dbHelper.getWritableDatabase(); // 获取可写数据库

                try {
                    if ("province".equals(type)) {
                        result = Utility.handleProvinceResponse(responseText, db); // 处理省份响应
                    } else if ("city".equals(type)) {
                        result = Utility.handleCityResponse(responseText, db, selectedProvince.getId()); // 处理城市响应
                    } else if ("county".equals(type)) {
                        result = Utility.handleCountyResponse(responseText, db, selectedCity.getId()); // 处理县区响应
                    }
                } finally {
                    db.close(); // 关闭数据库
                }

                if (result) {
                    // 如果处理成功，在UI线程中刷新数据
                    getActivity().runOnUiThread(() -> {
                        closeProgressDialog(); // 关闭加载对话框
                        if ("province".equals(type)) {
                            queryProvinces(); // 重新查询省份
                        } else if ("city".equals(type)) {
                            queryCities(); // 重新查询城市
                        } else if ("county".equals(type)) {
                            queryCounties(); // 重新查询县区
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "网络请求失败", e); // 打印错误日志
                // 在UI线程中显示错误信息
                getActivity().runOnUiThread(() -> {
                    closeProgressDialog(); // 关闭加载对话框
                    Toast.makeText(getContext(), "加载失败：" + e.getMessage(), Toast.LENGTH_SHORT).show(); // 显示吐司
                });
            }
        });
    }

    /**
     * 显示加载进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity()); // 创建进度对话框
            progressDialog.setMessage("正在加载..."); // 设置消息
            progressDialog.setCanceledOnTouchOutside(false); // 设置不能通过点击外部取消
        }
        progressDialog.show(); // 显示进度对话框
    }

    /**
     * 关闭加载进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss(); // 取消进度对话框
        }
    }
}
