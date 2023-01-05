package com.example.swiftest.speedtest;

import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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



}
