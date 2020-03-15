package com.example.chinaweather.util;

import android.text.TextUtils;

import com.example.chinaweather.db.City;
import com.example.chinaweather.db.County;
import com.example.chinaweather.db.Province;
import com.example.chinaweather.gson.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utility {

    /**
     * 解析和处理服务器返回的省级数据
     */
    public static boolean handleProvinceResponse(String response) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allProvinces = new JSONArray(response);

                for (int i = 0; i < allProvinces.length(); i++) {
                    JSONObject provinceObject = allProvinces.getJSONObject(i);//
                    Province province = new Province();
                    province.setProvinceName(provinceObject.getString("name"));//省或直辖市名字
                    province.setProvinceCode(provinceObject.getInt("id"));//id号
                    province.save();//保存
                }

                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        return false;
    }

    /**
     * 解析和处理服务器返回的市级数据
     */
    public static boolean handleCityResponse(String response, int provinceId) {
        if (!TextUtils.isEmpty(response)) {//字符串是否为空
            try {
                JSONArray allCities = new JSONArray(response);
                for (int i = 0; i < allCities.length(); i++) {
                    JSONObject cityObject = allCities.getJSONObject(i);//从id 位置开始读取
                    City city = new City();
                    city.setCityName(cityObject.getString("name"));//传入名字
                    city.setCityCode(cityObject.getInt("id"));//传入id
                    city.setProvinceId(provinceId);//传入当前市所属省的值
                    city.save();//保存数据
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的县级数据
     */
    public static boolean handleCountyResponse(String response, int cityId) {


        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCounties = new JSONArray(response);
                for (int i = 0; i < allCounties.length(); i++) {
                    JSONObject countyObject = allCounties.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 将返回的JSON数据解析成Weather实体类
     */
    public static Weather handleWeatherResponse(String response/*, int cityId*/) {

        try {
            JSONObject jsonObject = new JSONObject(response);//解析json数据
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");
            String weatherContent = jsonArray.getJSONObject(0).toString();//
            return new Gson().fromJson(weatherContent, Weather.class);//返回解析的对象
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

/*
        if (TextUtils.isEmpty(response)) {
            try {
                JSONArray allCouties = new JSONArray(response);
                for (int i = 0; i < allCouties.length(); i++) {
                    JSONObject countyObject = allCouties.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
*/

    }
}