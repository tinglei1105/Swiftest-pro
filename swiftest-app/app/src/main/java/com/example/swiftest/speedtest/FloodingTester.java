package com.example.swiftest.speedtest;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.Build;
import android.util.Log;

import com.blankj.utilcode.util.NetworkUtils;
import com.hjq.language.MultiLanguages;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class FloodingTester implements BandwidthTestable {
    //final static private String MasterIP = "192.168.31.247";
    final static private String MasterIP = "124.223.41.138";
    //final static private String MasterIP = "118.31.164.30";
    public static String TAG="FloodingTester";
    final static private int ThreadNum = 4;
    final static private int ServerCapability = 100;            // 100Mbps per server
    final static private int TestTimeout = 2000;                // Maximum test duration
    final static private int MaxTrafficUse = 200;               // Maximum traffic limit
    final static private int SamplingInterval = 20;             // Time interval for Sampling
    final static private int SamplingWindow = 100;               // Sampling overlap

    public String networkType;
    public List<MyNetworkInfo.CellInfo> cellInfo;
    public MyNetworkInfo.WifiInfo wifiInfo;
    public double bandwidth_Mbps;
    public double duration_s;
    public double traffic_MB;
    public String client_ip;
    public ArrayList<Double> speedSample = new ArrayList<>();
    Context context;
    private boolean stop = true;
    private ArrayList<String> ipList;

    public FloodingTester(Context context) {
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

        networkType = NetworkUtil.getNetworkType(context);
        IPListGetter ipListGetter = new IPListGetter();
        ipListGetter.start();
        ipListGetter.join();
        ipList = ipListGetter.getIpList();
        client_ip = ipListGetter.client_ip;
        String test_key = String.format(MultiLanguages.getAppLanguage(),
                "%d%s", System.currentTimeMillis(), TestUtil.getRandomString(3));
        DownloadThreadMonitor downloadThreadMonitor = new DownloadThreadMonitor(ipList, networkType, test_key);


        SimpleChecker checker = new SimpleChecker(speedSample);

        int uid;
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), 0);
            uid = info.uid;
        } catch (PackageManager.NameNotFoundException e) {
            uid = -1;
        }
        long startTime = System.currentTimeMillis();
        long previous = TrafficStats.getUidRxBytes(uid);
        downloadThreadMonitor.start();
        checker.start();

        ArrayList<Double> sizeRecord = new ArrayList<>();
        ArrayList<Long> timeRecord = new ArrayList<>();

        int posRecord = -1;
        long downloadSize = 0;
        while (true) {
            try {
                Thread.sleep(SamplingInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (checker.finish) {
                Log.d("Bandwidth Test", "Test succeed.");
                break;
            }

            if (stop) {
                Log.d("Bandwidth Test", "Testing Stopped.");
                break;
            }
            downloadSize = 0;
            for (DownloadThread t : downloadThreadMonitor.downloadThread)
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

            if (nowTime - startTime >= TestTimeout) {
                Log.d("Bandwidth Test", "Exceeding the time limit.");
                break;
            }
            if (downloadSizeMBits / 8 >= MaxTrafficUse) {
                Log.d("Bandwidth Test", "Exceeding the traffic limit.");
                break;
            }
        }
        duration_s = (double) (System.currentTimeMillis() - startTime) / 1000;
        downloadThreadMonitor.stop();
        checker.interrupt();
        checker.join();

        long after = TrafficStats.getUidRxBytes(uid);
        double total_size = (double) (after - previous) / 1024 / 1024;

        bandwidth_Mbps = checker.getSpeed();
        traffic_MB = sizeRecord.get(sizeRecord.size() - 1) / 8;

        double longTail = total_size - traffic_MB;

        TestResult result = TestResult.builder().withBandwidth(bandwidth_Mbps).
                withDuration(duration_s).withTraffic(traffic_MB).withLongTail(longTail).
                withNetworkType(networkType).
                withNetworkOperator(NetworkUtils.getNetworkOperatorName()).
                withPrivateIP(NetworkUtils.getIPAddress(true)).
                withPublicIP(client_ip).build();

        TestUtil.uploadDataUsage(test_key, downloadSize);
        Log.d(TAG, result.toString());

        return result;
    }

    @Override
    public void stop() {
        stop = true;
    }


    // 发送一个包后持续接收包，直到stopped被标记为true
    static class DownloadThread extends Thread {
        DatagramSocket socket;
        InetAddress address;
        String key;
        boolean stopped;
        int port;
        int size;


        DownloadThread(String ip, int port, String key) {
            try {
                this.address = InetAddress.getByName(ip);
                this.port = port;
                this.stopped = false;
                this.socket = new DatagramSocket();
                this.key = key;
                socket.setSoTimeout(TestTimeout);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            byte[] send_data = key.getBytes();
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
                    //Log.d("Download Thread", String.format("receive %d",size));
                }

                socket.send(stop_packet);
                //Log.d("Download Thread","Send stop");
                socket.close();

            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }

    // 包含一系列DownloadThread，控制这些Thread的开启与停止
    static class DownloadThreadMonitor {
        ArrayList<DownloadThread> downloadThread;
        ArrayList<String> serverIP;
        String key;
        int warmupNum;
        int stepNum;
        int serverNum;
        int runningServerNum;


        DownloadThreadMonitor(ArrayList<String> serverIP, String networkType, String key) {
            this.key = key;
            this.serverIP = serverIP;
            this.downloadThread = new ArrayList<>();
            for (String ip : serverIP)
                for (int i = 0; i < ThreadNum; ++i)
                    downloadThread.add(new DownloadThread(ip, 9876, key));

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
            for (DownloadThread thread : downloadThread)
                thread.stopped = true;
            for (DownloadThread thread : downloadThread)
                thread.join();
        }

        public int capability() {
            return runningServerNum * ServerCapability;
        }
    }

}
