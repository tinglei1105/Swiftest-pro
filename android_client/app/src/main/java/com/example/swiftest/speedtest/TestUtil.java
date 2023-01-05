package com.example.swiftest.speedtest;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public class TestUtil {
    public static void uploadTestResult(TestResult result) {
        try {
            URL url = new URL("http://124.223.41.138:8080/speedtest/data/upload");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            JSONObject jsonParam = new JSONObject();
            //"bandwidth":10.5,
            //    "duration":0.5,
            //    "traffic":2.1
            jsonParam.put("bandwidth", result.bandwidth);
            jsonParam.put("duration", result.duration);
            jsonParam.put("traffic", result.traffic);

            Log.i("JSON", jsonParam.toString());
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(jsonParam.toString());

            os.flush();
            os.close();

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

    public  static String toJsonArrayString(List<?> arr) throws JSONException, IllegalAccessException {
        JSONArray jsonArray=new JSONArray();
        for(Object obj:arr){
            jsonArray.put(toJsonObject(obj));
        }
        return jsonArray.toString();
    }
}
