package com.example.swiftest.speedtest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

public class FloodingTester implements BandwidthTestable{
    Context context;
    private boolean stop = true;
    public String networkType;
    //final static private String MasterIP = "192.168.31.247";
    final static private String MasterIP = "124.223.41.138";
    //final static private String MasterIP = "118.31.164.30";
    final static private int ThreadNum = 4;
    final static private int ServerCapability = 100;            // 100Mbps per server

    final static private int TestTimeout = 2000;                // Maximum test duration
    final static private int MaxTrafficUse = 200;               // Maximum traffic limit

    final static private int SamplingInterval = 20;             // Time interval for Sampling
    final static private int SamplingWindow = 100;               // Sampling overlap

    final static private int CheckerSleep = 50;                 // Time interval between checks
    final static private int CheckerWindowSize = 10;            // SimpleChecker window size
    final static private int CheckerSelectedSize = 8;           // SimplerChecker selected size
    final static private double CheckerThreshold = 0.08;        // threshold
    final static private int CheckerTimeoutWindow = 50;         // Window size when overtime

    public double bandwidth_Mbps;
    public double duration_s ;
    public double traffic_MB ;

    public ArrayList<Double> speedSample = new ArrayList<>();

    public FloodingTester(Context context){
        this.context = context;
    }

    @Override
    public TestResult test() throws IOException, InterruptedException {
        stop = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d("No permission:", "ACCESS_FINE_LOCATION");
                return new TestResult();
            }
        }

        networkType = TestUtil.getNetworkType(context);

        FloodingTester.InitThread initThread = new FloodingTester.InitThread();
        initThread.start();
        initThread.join();

        DownloadThreadMonitor downloadThreadMonitor = new DownloadThreadMonitor(initThread.ipList, networkType);


        FloodingTester.SimpleChecker checker = new FloodingTester.SimpleChecker(speedSample);

        long startTime = System.currentTimeMillis();
        downloadThreadMonitor.start();
        checker.start();

        ArrayList<Double> sizeRecord = new ArrayList<>();
        ArrayList<Long> timeRecord = new ArrayList<>();
        int posRecord = -1;
        while (true) {
            try {
                Thread.sleep(SamplingInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long downloadSize = 0;
            for (FloodingTester.DownloadThread t : downloadThreadMonitor.downloadThread)
                downloadSize += t.size;
            double downloadSizeMBits = (double) (downloadSize) / 1024 / 1024 * 8;
            long nowTime = System.currentTimeMillis();

            while (posRecord + 1 < timeRecord.size() && nowTime - timeRecord.get(posRecord + 1) >= SamplingWindow)
                posRecord++;

            double speed = 0;
            if (posRecord >= 0)
                speed = (downloadSizeMBits - sizeRecord.get(posRecord)) * 1000.0 / (nowTime - timeRecord.get(posRecord));
            speedSample.add(speed);

            sizeRecord.add(downloadSizeMBits);
            timeRecord.add(nowTime);

            if (speed > downloadThreadMonitor.capability())
                downloadThreadMonitor.add();

            if (checker.finish) {
                Log.d("Bandwidth Test", "Test succeed.");
                break;
            }
            if (nowTime - startTime >= TestTimeout) {
                Log.d("Bandwidth Test", "Exceeding the time limit.");
                break;
            }
            if (downloadSizeMBits / 8 >= MaxTrafficUse) {
                Log.d("Bandwidth Test", "Exceeding the traffic limit.");
                break;
            }
            if (stop) {
                Log.d("Bandwidth Test", "Testing Stopped.");
                break;
            }
        }
        downloadThreadMonitor.stop();
        checker.interrupt();
        checker.join();

        bandwidth_Mbps = checker.getSpeed();
        duration_s = (double) (System.currentTimeMillis() - startTime) / 1000;
        traffic_MB = sizeRecord.get(sizeRecord.size() - 1) / 8;

        TestResult result = new TestResult(bandwidth_Mbps,duration_s,traffic_MB);
        TestUtil.uploadTestResult(result);
        return result;
    }

    @Override
    public void stop() {
        stop = true;
    }

    // InitThread 的工作是获取一个可用的ip列表，然后ping每个服务器，根据rtt排序
    static class InitThread extends Thread {
        public ArrayList<String> ipList;

        final static private int InitTimeout = 500;
        final static private int MinFinishedNum = 5;
        InitThread() {
            this.ipList = new ArrayList<>();
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
                    ArrayList<FloodingTester.PingThread> pingThreads = new ArrayList<>();
                    for (String ip : ipList)
                        pingThreads.add(new FloodingTester.PingThread(ip));
                    for (FloodingTester.PingThread pingThread : pingThreads)
                        pingThread.start();

                    long start_time = System.currentTimeMillis();
                    while (true) {
                        int finish_num = 0;
                        for (FloodingTester.PingThread pingThread : pingThreads)
                            if (pingThread.finished)
                                finish_num++;
                        if (finish_num == server_num)
                            break;
                        if (System.currentTimeMillis() - start_time > InitTimeout && finish_num > MinFinishedNum)
                            break;
                    }

                    Collections.sort(pingThreads);

                    for (FloodingTester.PingThread pingThread : pingThreads)
                        this.ipList.add(pingThread.ip);
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // ping一个特定的地址，然后记录rtt
    static class PingThread extends Thread implements Comparable<FloodingTester.PingThread> {
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
        public int compareTo(FloodingTester.PingThread pingThread) {
            return Long.compare(rtt, pingThread.rtt);
        }
    }

    // 发送一个包后持续接收包，直到stopped被标记为true
    static class DownloadThread extends Thread {
        DatagramSocket socket;
        InetAddress address;
        boolean stopped;
        int port;
        int size;


        DownloadThread(String ip, int port) {
            try {
                this.address = InetAddress.getByName(ip);
                this.port = port;
                this.stopped = false;
                this.socket = new DatagramSocket();
                socket.setSoTimeout(TestTimeout);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            byte[] send_data = "1".getBytes();
            DatagramPacket send_packet = new DatagramPacket(send_data, send_data.length, address, port);
            byte[] stop_data = "stop".getBytes();
            DatagramPacket stop_packet = new DatagramPacket(stop_data, stop_data.length, address, port);
            try {
                socket.send(send_packet);

                int BUFFER_SIZE = 1024;
                byte[] receive_buf = new byte[BUFFER_SIZE * 2];
                DatagramPacket receive_packet = new DatagramPacket(receive_buf, receive_buf.length);

                while (!stopped) {
                    socket.receive(receive_packet);
                    String receive_data = new String(receive_packet.getData(), 0, receive_packet.getLength());
                    size += receive_data.length();
                    Log.d("Download Thread", String.format("receive %d",size));
                }

                socket.send(stop_packet);
                Log.d("Download Thread","Send stop");
                socket.close();

            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }
    // 包含一系列DownloadThread，控制这些Thread的开启与停止
    static class DownloadThreadMonitor {
        ArrayList<FloodingTester.DownloadThread> downloadThread;
        ArrayList<String> serverIP;
        int warmupNum;
        int stepNum;
        int serverNum;
        int runningServerNum;


        DownloadThreadMonitor(ArrayList<String> serverIP, String networkType) {
            this.serverIP = serverIP;
            this.downloadThread = new ArrayList<>();
            for (String ip : serverIP)
                for (int i = 0; i < ThreadNum; ++i)
                    downloadThread.add(new FloodingTester.DownloadThread(ip, 9876));

            this.serverNum = serverIP.size();
            this.runningServerNum = 0;

            switch (networkType) {
                case "5G":
                    this.warmupNum = 5;
                    this.stepNum = 3;
                    break;
                case "WiFi":
                    this.warmupNum = 2;
                    this.stepNum = 2;
                    break;
                case "4G":
                    this.warmupNum = 2;
                    this.stepNum = 1;
                    break;
                default:
                    this.warmupNum = 1;
                    this.stepNum = 1;
                    break;
            }
        }

        void start() {
            for (int i = 0; i < warmupNum; ++i) {
                if (runningServerNum >= serverNum)
                    return;
                for (int j = 0; j < ThreadNum; ++j)
                    downloadThread.get(i * ThreadNum + j).start();
                Log.d("Server added:", serverIP.get(runningServerNum));
                runningServerNum++;
            }
        }

        public void add() {
            for (int i = 0; i < stepNum; ++i) {
                if (runningServerNum >= serverNum)
                    return;
                for (int j = 0; j < ThreadNum; ++j)
                    downloadThread.get(runningServerNum * ThreadNum + j).start();
                Log.d("Server added:", serverIP.get(runningServerNum));
                runningServerNum++;
            }
        }

        public void stop() throws InterruptedException {
            for (FloodingTester.DownloadThread thread : downloadThread)
                thread.stopped = true;
            for (FloodingTester.DownloadThread thread : downloadThread)
                thread.join();
        }

        public int capability() {
            return runningServerNum * ServerCapability;
        }
    }

    // 检查采样的样本，在稳定是停止测速
    static class SimpleChecker extends Thread {
        ArrayList<Double> speedSample;
        boolean finish;
        Double simpleSpeed;

        SimpleChecker(ArrayList<Double> speedSample) {
            this.speedSample = speedSample;
            this.finish = false;
            this.simpleSpeed = 0.0;
        }

        public void run() {
            while (!finish) {
                try {
                    sleep(CheckerSleep);

                    int n = speedSample.size();
                    if (n < CheckerWindowSize) continue;

                    ArrayList<Double> recentSamples = new ArrayList<>();
                    for (int i = n - CheckerWindowSize; i < n; ++i)
                        recentSamples.add(speedSample.get(i));
                    Collections.sort(recentSamples);
                    int windowNum = CheckerWindowSize - CheckerSelectedSize + 1;
                    for (int i = 0; i < windowNum; ++i) {
                        int j = i + CheckerSelectedSize - 1;
                        double lower = recentSamples.get(i), upper = recentSamples.get(j);
                        // Here no division by 0 is considered,
                        // but (NaN < CheckerThreshold) so it's work!
                        // All 0 should not go through this condition.
                        if ((upper - lower) / upper < CheckerThreshold) {
                            double res = 0;
                            for (int k = i; k <= j; ++k)
                                res += recentSamples.get(k);
                            simpleSpeed = res / CheckerSelectedSize;
                            finish = true;
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    double res = 0.0;
                    int n = speedSample.size();
                    for (int k = n - CheckerTimeoutWindow; k < n; ++k)
                        res += speedSample.get(k);
                    simpleSpeed = res / CheckerTimeoutWindow;
                    break;
                }
            }
        }

        public double getSpeed() {
            return simpleSpeed;
        }
    }
}
