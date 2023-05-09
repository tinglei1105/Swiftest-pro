package com.example.swiftest.speedtest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.hjq.language.MultiLanguages;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class PacketTrainTester {
    Context context;
    // TODO 把这些通用的字段都抽象到BandwidthTestable里面
    public String networkType;
    private ArrayList<String> ipList;
    public String client_ip;
    public int startSendSpeed=100; //Mbps
    int maxSendSpeed=600;
    public int sendTime=100; // ms
    double paramK=1.2;
    int paramM=3;
    static String TAG="PacketTrainTester";
    TestListener testListener;
    public  PacketTrainTester(Context context){
        this.context=context;
    }
    public TestResult test() throws InterruptedException, IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d("No permission:", "ACCESS_FINE_LOCATION");

                // TODO: throw an error?
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
        ArrayList<Controller>controllers=new ArrayList<>();
        //long startTime=System.currentTimeMillis();
        int counter=0;

        AdvancedClient client=new AdvancedClient(test_key,ipList.get(0));
        try {
            client.connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        long startTime=System.currentTimeMillis();
        ArrayList<Double>resultList=new ArrayList<>();
        List<Integer> speedList;
        int speedStage=0;
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
        int sendSpeed= speedList.get(speedStage);
        while (true){
            UDPReceiver receiver=new UDPReceiver(client.udpSock);
            receiver.start();
            Log.d(TAG, String.format("send speed:%d, counter:%d",sendSpeed,counter));
            client.startSend(sendSpeed,sendTime);
            Log.d(TAG, "test: stop check");
            receiver.join();
            Log.d(TAG, "test: stop receive");
            long duration=receiver.endTime-receiver.startTime;
            Log.d(TAG, String.format("duration:%d",duration));
            //double speed=(double) receiver.byteCount*1000/duration/1024/1024*8;
            double speed=receiver.speedMid;
            double speedMax=receiver.speedMax;
            Log.d(TAG,String.format("send speed: %d, actual speed:%.2f",sendSpeed,speed));
            if(duration==0||speed==0||speedMax==0){
                continue;
            }
            if( speedMax > sendSpeed/paramK){
                Log.d(TAG, "test: not saturated");
                speedStage++;
                if(speedStage<speedList.size()){
                    sendSpeed= speedList.get(speedStage);
                }else{
                    sendSpeed+=100;
                }
                counter=0;
                resultList.clear();
                if(sendSpeed>maxSendSpeed){
                    Log.d(TAG, "test: cannot test");
                    break;//cannot test
                }
                if(testListener!=null){
                    testListener.process("increase speed\n");
                }
                //Thread.sleep(50);
                continue;
            }
            counter++;
            if(testListener!=null){
                testListener.process(String.format("send speed:%d,count:%d,speed %.2f\n",sendSpeed,counter,speed));
            }
            resultList.add(speed);
            if(counter==paramM)break;
            //Thread.sleep(50);
        }
        double duration=(double)(System.currentTimeMillis()-startTime)/1000;
        double traffic=(double)client.getUsage()/1024/1024;
        double server_usage=(double) client.getUsage()/1024/1024;
        client.end();
        Log.d(TAG, resultList.toString());
        double sum=0;
        for(Double num:resultList){
            sum+=num;
        }
        double result=0;
        if(resultList.size()>0){
            result=sum/resultList.size();
        }

        TestResult testResult=TestResult.builder().withBandwidth(result).withDuration(duration)
                .withTraffic(traffic).withServerUsage(server_usage).build();
        return testResult;
    }
    public void setOnTestListener(TestListener listener){
        this.testListener=listener;
    }
    public  static abstract class TestListener{
        public void process(String output){

        }
    }
    //  只用于接收是否完成
    static class Checker extends Thread{
        UDPReceiver receiver;
        public Checker(UDPReceiver receiver){
            this.receiver=receiver;
        }

        @Override
        public void run() {
             long lastSize = receiver.byteCount;
            do {
                try {
                    lastSize = receiver.byteCount;
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (receiver.byteCount != lastSize || receiver.byteCount == 0);
        }
    }

    // 用于发送控制信息，包含TCP和UDP socket
    static class Controller {
        Socket tcpSocket;
        DatagramSocket udpSocket;
        InetAddress address;
        int tcpPort;
        int udpPort;
        String key;
        long startTime;
        long endTime;
        Controller(String key,String address,int tcpPort,int udpPort) throws UnknownHostException {
            this.address=InetAddress.getByName(address);
            this.tcpPort=tcpPort;
            this.udpPort=udpPort;
            this.key=key;
        }
        void connect() throws IOException, InterruptedException {
            tcpSocket=new Socket(address,tcpPort);
            udpSocket=new DatagramSocket();
            byte[] send_data=key.getBytes();
            tcpSocket.getOutputStream().write(send_data);
            Thread.sleep(50);

            DatagramPacket send_packet=new DatagramPacket(send_data,send_data.length,address,udpPort);
            for(int i=0;i<5;i++){
                udpSocket.send(send_packet);
            }
            Log.d(TAG, "connect: UDP sent");

            int BUFFER_SIZE = 1024;
            byte[] receive_buf = new byte[BUFFER_SIZE * 2];
            // 获取ACK
            int sz=tcpSocket.getInputStream().read(receive_buf);
            Log.d("packet train controller", new String(receive_buf,0,sz));
        }
        void test(int sendBandwidth, int sendMs) throws IOException, InterruptedException {
            UDPReceiver receiver= new UDPReceiver(udpSocket);
            Checker checker= new Checker(receiver);
            String message=String.format("%d,%d",sendBandwidth,sendMs);
            checker.start();
            receiver.start();
            tcpSocket.getOutputStream().write(message.getBytes());
            checker.join();
            receiver.interrupt();
            int BUFFER_SIZE = 1024;
            byte[] receive_buf = new byte[BUFFER_SIZE * 2];
            int sz=tcpSocket.getInputStream().read(receive_buf);
            Log.d("packet train send",new String(receive_buf,0,sz));
            Log.d("packet train receive", String.valueOf(receiver.byteCount));
            startTime= receiver.startTime;
            endTime=receiver.endTime;
        }
    }
    static class TestThread extends Thread{
        Controller controller;
        int sendBandWidth;
        int sendMs;

        public TestThread(Controller controller,int sendBandWidth,int sendMs){
            this.controller=controller;
            this.sendBandWidth=sendBandWidth;
            this.sendMs=sendMs;
        }

        @Override
        public void run() {
            try {
                controller.test(sendBandWidth,sendMs);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
