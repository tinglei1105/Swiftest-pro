package com.example.swiftest.speedtest;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

public class IPListGetter extends Thread{
    public ArrayList<String> ipList;

    final static private int InitTimeout = 500;
    final static private int MinFinishedNum = 5;
    final static private String MasterIP = "124.223.41.138";
    public IPListGetter() {
        this.ipList = new ArrayList<>();
    }
    public ArrayList<String> getIpList(){
        return ipList;
    }
    public void run() {
        try {
            URL url = new URL("http://" + MasterIP + ":8080/speedtest/iplist/available");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(InitTimeout);
            connection.setReadTimeout(InitTimeout);
            connection.connect();

            if (connection.getResponseCode() == 200) {
                InputStream inputStream = connection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null)
                    stringBuilder.append(line);

                JSONObject jsonObject = new JSONObject(stringBuilder.toString());

                int server_num = jsonObject.getInt("server_num");
                JSONArray jsonArray = jsonObject.getJSONArray("ip_list");
                ArrayList<String> ipList = new ArrayList<>();
                for (int i = 0; i < server_num; ++i)
                    ipList.add(jsonArray.getString(i));
                connection.disconnect();
                Log.d("Init", ipList.toString());
                ArrayList<PingThread> pingThreads = new ArrayList<>();
                for (String ip : ipList)
                    pingThreads.add(new PingThread(ip));
                for (PingThread pingThread : pingThreads)
                    pingThread.start();

                long start_time = System.currentTimeMillis();
                while (true) {
                    int finish_num = 0;
                    for (PingThread pingThread : pingThreads)
                        if (pingThread.finished)
                            finish_num++;
                    if (finish_num == server_num)
                        break;
                    if (System.currentTimeMillis() - start_time > InitTimeout && finish_num > MinFinishedNum)
                        break;
                }

                Collections.sort(pingThreads);

                for (PingThread pingThread : pingThreads)
                    this.ipList.add(pingThread.ip);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    // ping一个特定的地址，然后记录rtt
    static class PingThread extends Thread implements Comparable<PingThread> {
        long rtt;
        String ip;
        boolean finished;
        final static private int PingTimeout = 5000;
        PingThread(String ip) {
            this.ip = ip;
            this.finished = false;
            this.rtt = PingTimeout * 2;
        }

        public void run() {
            try {
                URL url = new URL("http://" + ip + ":8080/ping");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(PingTimeout);
                connection.setReadTimeout(PingTimeout);
                long nowTime = System.currentTimeMillis();
                connection.connect();

                connection.getResponseCode();
                rtt = System.currentTimeMillis() - nowTime;
                Log.d("PING Thread", String.format("rtt: %d",rtt));
                connection.disconnect();
                finished = true;
            } catch (IOException e) {
                finished = true;
            }
        }

        @Override
        public int compareTo(PingThread pingThread) {
            return Long.compare(rtt, pingThread.rtt);
        }
    }
}
