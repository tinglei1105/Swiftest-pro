package com.example.swiftest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

public class BandwidthTest {

    Context context;

    final static private int InitTimeout = 500;
    final static private int PingTimeout = 5000;
    final static private int MinFinishedNum = 5;

    final static private int ThreadNum = 1;                     // download threads for each server
    final static private int ServerCapability = 100;            // 100Mbps per server

    final static private int TestTimeout = 5000;               // Maximum test duration
    final static private int MaxTrafficUse = 200;               // Maximum traffic limit

    final static private int SamplingInterval = 20;             // Time interval for Sampling
    final static private int SamplingWindow = 100;              // Sampling overlap

    final static private int CheckerSleep = 50;                 // Time interval between checks
    final static private int CheckerWindowSize = 10;            // SimpleChecker window size
    final static private int CheckerSelectedSize = 8;           // SimplerChecker selected size
    final static private double CheckerThreshold = 0.08;        // threshold
    final static private int CheckerTimeoutWindow = 50;         // Window size when overtime


    final static private String MasterIP = "118.31.164.30";

    public String bandwidth_Mbps = "0";
    public String duration_s = "0";
    public String traffic_MB = "0";
    public String networkType;
    static public ArrayList<Double> speedSample = new ArrayList<>();

    boolean stop = false;

    BandwidthTest(Context context) {
        this.context = context;
    }

    public void stop() {
        stop = true;
    }

    static class PingThread extends Thread implements Comparable<PingThread> {
        long rtt;
        String ip;
        boolean finished;

        PingThread(String ip) {
            this.ip = ip;
            this.finished = false;
            this.rtt = PingTimeout * 2;
        }

        public void run() {
            try {
                URL url = new URL("http://" + ip + "/testping.html");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(PingTimeout);
                connection.setReadTimeout(PingTimeout);
                long nowTime = System.currentTimeMillis();
                connection.connect();
                connection.getResponseCode();
                rtt = System.currentTimeMillis() - nowTime;
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

    static class InitThread extends Thread {
        public ArrayList<String> ipList;

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
                    Log.d("server_num", String.valueOf(server_num));
                    Log.d("ip_list", String.valueOf(ipList));

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
    }

    class CISChecker extends Thread {
        ArrayList<Double> speedSample;
        boolean finish;
        Double CISSpeed;
        Object lock;
        int CISSleep = 200;// ms
        int KSimilar = 3;
        double Threshold = 0.9;

        CISChecker(ArrayList<Double> speedSample, Object lock) {
            this.speedSample = speedSample;
            this.finish = false;
            this.CISSpeed = 0.0;
            this.lock = lock;
        }

        public void run() {
            int similarCnt = 0;
            double lastUp = 0;
            double lastDown = 0;
            long startTime = System.currentTimeMillis();
            while (!finish) {
                try {
                    sleep(CISSleep);
                    synchronized (lock) {
                        Collections.sort(speedSample);
                    }
                    int bias = 0;
                    while (bias<speedSample.size() && speedSample.get(bias) == 0) bias++;
//                    Log.d("bias:", String.valueOf(bias));
                    int n = speedSample.size() - bias;
                    if (n <= 2) {
                        continue;
                    }
//                    Log.d("speedSample",speedSample.toString());
                    double up = 0;
                    double down = 0;
                    double k2l = 0;
                    double minInterval = (speedSample.get(n - 1 + bias) - speedSample.get(0 + bias)) / (n - 1);
//                    Log.d("minInterval", String.valueOf(minInterval));

                    for (int i = 0; i < n; i++) {
                        for (int j = i + 1; j < n; j++) {
                            double k2ltemp = (j - i + 1) * (j - i + 1) / Math.max((speedSample.get(j + bias) - speedSample.get(i + bias)), minInterval);
                            if (k2ltemp > k2l) {
                                k2l = k2ltemp;
                                up = speedSample.get(j + bias);
                                down = speedSample.get(i + bias);
                            }
                        }
                    }
                    double res = 0;
                    double cnt = 0;
                    for (int i = 0; i < n; i++) {
                        if (speedSample.get(i + bias) >= down && speedSample.get(i + bias) <= up) {
                            res += speedSample.get(i + bias);
                            cnt++;
                        }
                    }
//                    Log.d("up and down", "up: " + up + " down: " + down);
//                    Log.d("CISSpeed", "res:"+res+" cnt:"+cnt+" Speed:"+String.valueOf(res/cnt));
                    CISSpeed = res / cnt;
                    if (isSimilarCIS(up, down, lastUp, lastDown)) {
                        similarCnt++;
                        if (similarCnt >= KSimilar) {
                            finish = true;
                        }
                    } else {
                        similarCnt = 0;
                    }
                    lastDown = down;
                    lastUp = up;
//                    if(System.currentTimeMillis() - startTime >= TestTimeout){
//                        finish = true;
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean isSimilarCIS(double up1, double down1, double up2, double down2) {
            double v = (Math.min(up1, up2) - Math.max(down1, down2));
            double u = (Math.max(up1, up2) - Math.min(down1, down2));
            if (v / u >= Threshold) return true;
            return false;
        }

        public double getSpeed() {
            return CISSpeed;
        }
    }

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

        @SuppressWarnings("InfiniteLoopStatement")
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
                }

                socket.send(stop_packet);
                socket.close();

            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }

    static class UdpDownloadThread extends Thread {
        DatagramSocket socket;
        String ip;
        boolean stop;
        long size;

        UdpDownloadThread(String ip) {
            try {
                socket = new DatagramSocket();
                this.ip = ip;
                stop = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                byte[] send_data = "start".getBytes();
                DatagramPacket send_packet = new DatagramPacket(send_data, send_data.length, InetAddress.getByName(ip), 9876);
                socket.send(send_packet);

                int BUFFER_SIZE = 1024;
                byte[] receive_buf = new byte[BUFFER_SIZE * 2];
                DatagramPacket receive_packet = new DatagramPacket(receive_buf, receive_buf.length);
                while (!stop) {
                    socket.receive(receive_packet);
                    String receive_data = new String(receive_packet.getData(), 0, receive_packet.getLength());
                    size += receive_data.length();
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void Stop() {
            stop = true;
        }
    }

    static class DownloadCtl {
        ArrayList<String> serverIP;
        ArrayList<Socket> ctlSocket;
        ArrayList<OutputStream> outputStreams;
        ArrayList<UdpDownloadThread> downloadThread;
        int serverNum;
        int step = 1;
        double eachServerSpeed = 200; // Mbps
        ArrayList<Double> speedWindow;
        double speedSum = 0;

        DownloadCtl(ArrayList<String> IP, String networkType) throws IOException {
            serverIP = IP;
            downloadThread = new ArrayList<>();
            ctlSocket = new ArrayList<>();
            outputStreams = new ArrayList<>();
            for (String ip : serverIP) {
                Socket socket = new Socket(ip, 8080);
                ctlSocket.add(socket);
                outputStreams.add(socket.getOutputStream());
                downloadThread.add(new UdpDownloadThread(ip));
            }
            serverNum = 1;
            speedWindow = new ArrayList<>();
        }

        void startRecv() throws IOException {
            for (int i = 0; i < serverNum; i++) {
                outputStreams.get(i).write("start".getBytes());
                downloadThread.get(i).start();
            }
        }

        void stopRecv() throws IOException, InterruptedException {
            for (int i = 0; i < serverIP.size(); i++) {
                outputStreams.get(i).write("stop ".getBytes());
                ctlSocket.get(i).close();
                downloadThread.get(i).Stop();
            }
            for (UdpDownloadThread t : downloadThread) {
                t.join();
            }
        }

        double capability() {
            return serverNum * eachServerSpeed;
        }

        void add() throws IOException {
            if (serverNum >= serverIP.size()) {
                return;
            }
            int newNum = Math.min(serverIP.size(), serverNum + step);
            for (int i = serverNum; i < newNum; i++) {
                outputStreams.get(i).write("start".getBytes());
                downloadThread.get(i).start();
            }
            serverNum = newNum;
        }

        void fitSpeed(double speed) throws IOException {
            if (speed > capability()) {
                add();
                speedWindow.clear();
                speedSum = 0;
            }
            speedWindow.add(speed);
            speedSum += speed;
            if (speedWindow.size() > 5) {
                speedSum -= speedWindow.get(0);
                speedWindow.remove(0);
            }
            if (speedSum > 0) {
                double newSpeed = (speedSum + capability()*(10-speedWindow.size())) / 10;
                for (int i = 0; i < serverNum; i++) {
                    String msg = "speed " + newSpeed/serverNum + " ";
                    Log.d("tcp send", msg);
                    outputStreams.get(i).write(msg.getBytes());
                }
            }
        }
    }


    static class Download extends Thread {
        DatagramSocket socket;
        String ip;
        int speed;
        int sendingTime;
        long dataSize; // speed*sendingTime
        long size;
        long receivingTime = 0;

        Download(String ip, int speed, int sendingTime) {
            try {
                socket = new DatagramSocket();
                this.ip = ip;
                this.speed = speed;
                this.sendingTime = sendingTime;
                this.dataSize = (long)((((long)speed/8)*1024*1024)*((double)sendingTime/1000)); // 单位byte
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            int BUFFER_SIZE = 1024;
            byte[] receive_buf = new byte[BUFFER_SIZE * 2];
            DatagramPacket receive_packet = new DatagramPacket(receive_buf, receive_buf.length);

            Thread checker = new Thread(() -> {
                long lastSize = size;
                do {
                    try {
                        lastSize = size;
                        sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (size != lastSize || size == 0);
//                Log.d("size changed", String.valueOf(size) + String.valueOf(lastSize));
            });
            Thread receiver = new Thread(() -> {
                try {
                    byte[] send_data = "start".getBytes();
                    DatagramPacket send_packet = new DatagramPacket(send_data, send_data.length, InetAddress.getByName(ip), 9876);
                    socket.send(send_packet);
                    Log.d("send", "trigger");

                    long startTime = 0;
//                    long lastTime = 0;
//                    long lastSize = 0;
                    Thread t = Thread.currentThread();
                    while (!t.isInterrupted()) {
                        socket.receive(receive_packet);
                        if (startTime == 0) {
                            startTime = System.currentTimeMillis();
                            Log.d("first packet arrived", String.valueOf(startTime));
                        }
                        long currentTime = System.currentTimeMillis();
                        receivingTime = currentTime - startTime;
                        String receive_data = new String(receive_packet.getData(), 0, receive_packet.getLength());
                        size += receive_data.length();
//                        Log.d("content", String.valueOf(receive_data.charAt(0)));
//                        Log.d("length", String.valueOf(receive_data.length()));
//                        if (currentTime - lastTime >= 10) {
////                            speedSample.add(((double)((size - lastSize)*8)/1024/1024)/((double)(currentTime - lastTime)/1000));
//                            lastTime = currentTime;
//                            lastSize = size;
//                        }
                    }
                    Log.d("receive time", String.valueOf(receivingTime));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            receiver.start();
            checker.start();
            try {
                checker.join();
                receiver.interrupt();
                Log.d("receive bytes", String.valueOf(size));
                Log.d("speedSample", String.valueOf(speedSample));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public int getReceivingTime() {
            Log.d("time", String.valueOf(receivingTime));
            return (int)receivingTime;
        }

        public double getSpeed() {
            Log.d("time", String.valueOf(receivingTime));
            return ((double)(size*8)/1024/1024)/((double)(receivingTime)/1000);
        }

        public double getTrafficMB() {
            return (double)size/1024/1024;
        }
    }

    /*
    1. Build tcp connection with each server
    2.
     */
    static class DownloadTest {
        ArrayList<String> serverIP;
        ArrayList<Socket> ctlSocket;
        ArrayList<OutputStream> outputStreams;
        final static int sendingTime = 100; // 每次以固定速率发送的时长
        double trafficMB = 0;
        boolean completed = false;

        DownloadTest(ArrayList<String> IP, String networkType) throws IOException {
            serverIP = IP;
            ctlSocket = new ArrayList<>();
            outputStreams = new ArrayList<>();
            for (String ip : serverIP) {
                Socket socket = new Socket(ip, 8080);
                ctlSocket.add(socket);
                outputStreams.add(socket.getOutputStream());
            }
        }

        int testWithSpeed(double speed) throws IOException, InterruptedException {
            int serverNum = (int) ((speed-1)/200) + 1;
            Log.d("server num", String.valueOf(serverNum));
            int eachServerSpeed = (int)(speed/serverNum);
            ArrayList<Download> downloads = new ArrayList<>();
            String msg = String.format(Locale.CHINA, "SET-%d-%d", eachServerSpeed, sendingTime);
            Log.d("set msg", msg);
            for (int i = 0; i < serverNum; i++) {
                Log.d("send set msg to server", String.valueOf(i));
                outputStreams.get(i).write(msg.getBytes());
                Download download = new Download(serverIP.get(i), eachServerSpeed, sendingTime);
                downloads.add(download);
            }
            for (Download download : downloads) {
                download.start();
            }
            int avgRecvTime = 0;
            for (Download download : downloads) {
                download.join();
                avgRecvTime += download.getReceivingTime();
                trafficMB += download.getTrafficMB();
            }
            avgRecvTime /= serverNum;
            return avgRecvTime;
        }

        public double test() throws IOException, InterruptedException {
            int sendSpeed = 200; // Mbps
            int recvTime = 0;
            while (!completed) {
                Log.d("start test", String.valueOf(sendSpeed));
                recvTime = testWithSpeed(sendSpeed);
                if (recvTime > 1.3*sendingTime) { // TODO:1.3 is set arbitrarily
                    completed = true;
                }
                if (sendSpeed + 200 > 200) { // TODO:only 1 server is running now
                    break;
                }
                sendSpeed += 200;
            }
            for (int i = 0; i < serverIP.size(); i++) {
                outputStreams.get(i).write("FIN".getBytes());
                ctlSocket.get(i).close();
            }
            return (sendingTime/(double)recvTime)*sendSpeed;
        }

        public double getTrafficMB() {
            return trafficMB;
        }
    }

    // non-flooding
    public void SpeedTestNew() throws InterruptedException, IOException {
        stop = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d("No permission:", "ACCESS_FINE_LOCATION");
                return;
            }
        }

        networkType = getNetworkType();

        InitThread initThread = new InitThread();
//        initThread.start();
//        initThread.join();
        // 手动指定ip list, 方便开发
        initThread.ipList = new ArrayList<>(Arrays.asList("81.70.55.189")); //"81.70.193.140",
        Log.d("ip list", String.valueOf(initThread.ipList));

        long startTime = System.currentTimeMillis();

        DownloadTest downloadTest = new DownloadTest(initThread.ipList, networkType);
        double result = downloadTest.test();
        bandwidth_Mbps = String.format(Locale.CHINA, "%.2f", result);
        duration_s = String.format(Locale.CHINA, "%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
        traffic_MB = String.format(Locale.CHINA, "%.2f", downloadTest.getTrafficMB());

        Log.d("bandwidth_Mbps", bandwidth_Mbps);
        Log.d("duration_s", duration_s);
        Log.d("traffic_MB", traffic_MB);
    }

    public void SpeedTest() throws InterruptedException, IOException {
        stop = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d("No permission:", "ACCESS_FINE_LOCATION");
                return;
            }
        }

        networkType = getNetworkType();

        InitThread initThread = new InitThread();
//        initThread.start();
//        initThread.join();

        initThread.ipList = new ArrayList<>(Arrays.asList("81.70.193.140", "81.70.55.189"));
        Log.d("ip list", String.valueOf(initThread.ipList));

//        DownloadThreadMonitor downloadThreadMonitor = new DownloadThreadMonitor(initThread.ipList, networkType);
        DownloadCtl downloadCtl = new DownloadCtl(initThread.ipList, networkType);

        SimpleChecker checker = new SimpleChecker(speedSample);
//        Object lock = new Object();
//        CISChecker checker = new CISChecker(speedSample, lock);

        long startTime = System.currentTimeMillis();
//        downloadThreadMonitor.start();
        downloadCtl.startRecv();
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
            for (UdpDownloadThread t : downloadCtl.downloadThread)
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

//            if (speed > downloadThreadMonitor.capability())
//                downloadThreadMonitor.add();
            if (nowTime - startTime >= 200) {
                downloadCtl.fitSpeed(speed);
            }

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
//        downloadThreadMonitor.stop();
        downloadCtl.stopRecv();
        checker.interrupt();
        checker.join();

        bandwidth_Mbps = String.format(Locale.CHINA, "%.2f", checker.getSpeed());
        duration_s = String.format(Locale.CHINA, "%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
        traffic_MB = String.format(Locale.CHINA, "%.2f", sizeRecord.get(sizeRecord.size() - 1) / 8);

        Log.d("bandwidth_Mbps", bandwidth_Mbps);
        Log.d("duration_s", duration_s);
        Log.d("traffic_MB", traffic_MB);
    }

    String getNetworkType() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isAvailable())
            return "NONE";
        int connectionType = networkInfo.getType();
        if (connectionType == ConnectivityManager.TYPE_WIFI)
            return "WiFi";
        if (connectionType == ConnectivityManager.TYPE_MOBILE) {
            int cellType = networkInfo.getSubtype();
            switch (cellType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:     // api< 8: replace by 11
                case TelephonyManager.NETWORK_TYPE_GSM:      // api<25: replace by 16
                    return "2G";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:   // api< 9: replace by 12
                case TelephonyManager.NETWORK_TYPE_EHRPD:    // api<11: replace by 14
                case TelephonyManager.NETWORK_TYPE_HSPAP:    // api<13: replace by 15
                case TelephonyManager.NETWORK_TYPE_TD_SCDMA: // api<25: replace by 17
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_LTE:      // api<11: replace by 13
                case TelephonyManager.NETWORK_TYPE_IWLAN:    // api<25: replace by 18
                case 19: // LTE_CA
                    return "4G";
                case TelephonyManager.NETWORK_TYPE_NR:       // api<29: replace by 20
                    return "5G";
                default:
                    return "unknown";
            }
        }
        return "unknown";
    }
}