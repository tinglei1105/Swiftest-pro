package com.example.swiftest.speedtest;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.Build;
import android.util.Log;

import com.hjq.language.MultiLanguages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdvancedFloodingTester implements BandwidthTestable {

    Context context;

    public String networkType;
    public String client_ip;
    public ArrayList<Double> speedSample = new ArrayList<>();

    private ArrayList<String> ipList;
    private boolean stop=true;
    static String TAG="AdvancedFloodingTester";

    final static private int SamplingInterval = 20;
    final static private int SamplingWindow = 100;

    final static  private  int TestTimeout=3000;
    public AdvancedFloodingTester(Context context){
        this.context=context;
    }

    public TestResult test() throws InterruptedException, IOException {
        stop=false;
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

        SimpleChecker checker = new SimpleChecker(speedSample);

        ReceiverMonitor receiverMonitor=new ReceiverMonitor(ipList,networkType,test_key);
        int uid;
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), 0);
            uid = info.uid;
        } catch (PackageManager.NameNotFoundException e) {
            uid = -1;
        }
        long previous = TrafficStats.getUidRxBytes(uid);
        long startTime = System.currentTimeMillis();
        checker.start();
        // 开始速度
        //receiverMonitor.changeSpeed(5);
        receiverMonitor.start();

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
            for (UDPReceiver receiver:receiverMonitor.receivers)
                downloadSize += receiver.byteCount;
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
            if(nowTime-startTime>=TestTimeout){
                break;
            }
            if (speed > receiverMonitor.sendSpeed*0.9){
                receiverMonitor.stop();
                //TODO 策略
                // 加多少
                //receiverMonitor.changeSpeed(receiverMonitor.sendSpeed+5);
                receiverMonitor.increaseSpeed();
                receiverMonitor.start();
            }

        }
        receiverMonitor.stop();
        double duration_s = (double) (System.currentTimeMillis() - startTime) / 1000;
        checker.interrupt();
        checker.join();

        Thread.sleep(50);
        int usage=receiverMonitor.getUsage();
        double usage_MB=(double)usage/1024/1024;
        receiverMonitor.end();
        long after = TrafficStats.getUidRxBytes(uid);
        double total_size = (double) (after - previous) / 1024 / 1024;
        double bandwidth_Mbps = checker.getSpeed();
        double traffic_MB = sizeRecord.get(sizeRecord.size() - 1) / 8;

        Log.d(TAG, String.format("server send:%f , client use: %f",usage_MB,traffic_MB));

        TestResult result=TestResult.builder().withBandwidth(bandwidth_Mbps).withNetworkType(networkType).
                withDuration(duration_s).withTraffic(traffic_MB).withLongTail(total_size-traffic_MB).withServerUsage(usage_MB).build();
        Log.d(TAG, result.toString());
        TestUtil.uploadSwiftestResult(result,speedSample);
        return  result;
    }
    @Override
    public void stop() {

    }
    class ReceiverMonitor {
        public ArrayList<UDPReceiver> receivers;
        ArrayList<AdvancedClient>clients;
        ArrayList<String>serverIP;
        String key;
        String networkType;
        int serverCapacity=200;
        int sendSpeed;
        int maxServer=5;
        List<Integer> speedList;
        int speedStep=0;
        ReceiverMonitor(ArrayList<String> serverIP,String networkType,String key){
            this.key=key;
            this.serverIP=serverIP;
            this.receivers=new ArrayList<>();
            this.clients=new ArrayList<>();
            this.networkType=networkType;

            switch (networkType) {
                case "5G":
                    speedList= Arrays.asList(172, 289, 485, 806);
                    break;
                case "WiFi":
                    speedList=Arrays.asList(106,309,401, 683,905);
                    break;
                case "4G":
                    speedList=Arrays.asList(28, 55, 284);
                    break;
                default:
                    speedList=Arrays.asList(20,100,200);
                    break;
            }
            this.sendSpeed=speedList.get(speedStep);
        }

        void start() throws IOException, InterruptedException {
            while(clients.size()*serverCapacity<sendSpeed &&clients.size()<maxServer){
                AdvancedClient client=new AdvancedClient(key,serverIP.get(clients.size()));
                Log.d(TAG,"connect client");
                client.connect();
                clients.add(client);
                UDPReceiver receiver=new UDPReceiver(client.udpSock);
                receivers.add(receiver);
                receiver.start();
                Log.d(TAG, "add client");
            }
            int speed=sendSpeed;
            for(AdvancedClient client:clients){
                if(speed>serverCapacity){
                client.startSend(serverCapacity,3000);
                }
                else if(speed>0){
                    client.startSend(speed,3000);
                }
                speed-=serverCapacity;
            }
        }

        void changeSpeed(int sendSpeed){
            this.sendSpeed=sendSpeed;
        }
        void increaseSpeed(){
            speedStep++;
            if(speedStep<speedList.size()){
                sendSpeed=speedList.get(speedStep);
            }else{
                sendSpeed+=serverCapacity;
            }
            Log.d(TAG, String.format("increaseSpeed: %d",sendSpeed));
        }
        int getUsage() throws IOException {
            int usage=0;
            for(AdvancedClient client:clients){
                usage+=client.getUsage();
            }
            return usage;
        }

        void stop() throws IOException {
            for(AdvancedClient client:clients){
                client.stopSend();
            }
        }
        void end() throws IOException {
            for(AdvancedClient client:clients){
                client.end();
            }
        }
    }

}
