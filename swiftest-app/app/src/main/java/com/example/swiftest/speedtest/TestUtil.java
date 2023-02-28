package com.example.swiftest.speedtest;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestUtil {
    public static String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
    public static void uploadTestResult(TestResult result,TestResult baseline, ArrayList<Double>speedSample,
                                        MyNetworkInfo networkInfo) {
        try {
            URL url = new URL("http://124.223.41.138:8080/speedtest/data/upload");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            JSONObject jsonParam = new JSONObject();
            jsonParam.put("bandwidth", result.bandwidth);
            jsonParam.put("duration", result.duration);
            jsonParam.put("traffic", result.traffic);
            jsonParam.put("speed_sample",new JSONArray(speedSample));
            jsonParam.put("network_type",networkInfo.networkType);
            jsonParam.put("cell_info",toJsonArray(networkInfo.cellInfo).toString());
            jsonParam.put("wifi_info",toJsonObject(networkInfo.wifiInfo).toString());
            if(baseline!=null){
                jsonParam.put("baseline",baseline.bandwidth);
            }

            jsonParam.put("long_tail",result.longTail);
            //jsonParam.put("wifi_info","{\"wifi_SSID\":\"\\\"程搓搓和邱摆摆\\\"\"}");
            Log.i("JSON", jsonParam.toString());
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            OutputStreamWriter writer=new OutputStreamWriter(os, StandardCharsets.UTF_8);
            //os.writeBytes(jsonParam.toString());
            writer.write(jsonParam.toString());
            //os.flush();
            //os.close();
            writer.flush();
            writer.close();

            Log.i("STATUS", String.valueOf(conn.getResponseCode()));
            Log.i("MSG", conn.getResponseMessage());

            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void uploadDataUsage(String key, long dataUsage){
        try {
            URL url = new URL("http://124.223.41.138:8080/report/client");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            JSONObject jsonParam = new JSONObject();
            jsonParam.put("key", key);
            jsonParam.put("count",dataUsage);

            //jsonParam.put("wifi_info","{\"wifi_SSID\":\"\\\"程搓搓和邱摆摆\\\"\"}");
            Log.i("JSON", jsonParam.toString());
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            OutputStreamWriter writer=new OutputStreamWriter(os, StandardCharsets.UTF_8);
            //os.writeBytes(jsonParam.toString());
            writer.write(jsonParam.toString());
            //os.flush();
            //os.close();
            writer.flush();
            writer.close();

            Log.i("STATUS", String.valueOf(conn.getResponseCode()));
            Log.i("MSG", conn.getResponseMessage());

            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String toJsonObjectString(Object obj) throws IllegalAccessException, JSONException {
        JSONObject jsonObject=toJsonObject(obj);

        return jsonObject.toString();
    }

    public static JSONObject toJsonObject(Object obj) throws IllegalAccessException, JSONException {
        JSONObject jsonObject=new JSONObject();

        for(Field field:obj.getClass().getFields()){
            if(field.get(obj)==null){
                continue;
            }
            Object fieldObj= field.get(obj);
            assert fieldObj != null;

            //Log.d("FIELD", String.format("%s %s",field.getName(),fieldObj.getClass()));
            if(fieldObj.getClass().isPrimitive() || fieldObj instanceof String){
                jsonObject.put(field.getName(),fieldObj);
            }else {
                jsonObject.put(field.getName(),toJsonObject(fieldObj));
            }
        }
        return jsonObject;
    }

    public  static JSONArray toJsonArray(List<?> arr) throws JSONException, IllegalAccessException {
        JSONArray jsonArray=new JSONArray();
        for(Object obj:arr){
            jsonArray.put(toJsonObject(obj));
        }
        return jsonArray;
    }
}
