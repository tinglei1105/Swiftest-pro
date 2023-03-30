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


public class PacketTrainTester {
    Context context;
    // TODO 把这些通用的字段都抽象到BandwidthTestable里面
    public String networkType;
    private ArrayList<String> ipList;
    public String client_ip;
    public int sendSpeed=200;
    public int sendTime=100;
    static String TAG="PacketTrainTester";
    public  PacketTrainTester(Context context){
        this.context=context;
    }
    public TestResult test() throws InterruptedException {
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
        for(int i=0;i<1&&i<ipList.size();i++){
            try {
                controllers.add(new Controller(test_key, ipList.get(i), 9878, 9877));
                controllers.get(i).connect();
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return new TestResult();
            }catch (IOException e){
                e.printStackTrace();
                return  new TestResult();
            }
            ArrayList<TestThread>testThreads = new ArrayList<>();
            for(int j=0;j<=i;j++){
                testThreads.add(new TestThread(controllers.get(j),sendSpeed,sendTime));
            }
            for(TestThread testThread:testThreads){
                testThread.start();
            }
            for(TestThread testThread:testThreads){
                testThread.join();
            }
            for(Controller controller:controllers){
                Log.d(TAG, String.format("%d-%d=%d",controller.endTime,controller.startTime,controller.endTime-controller.startTime));
            }
            Thread.sleep(100);
        }
        return new TestResult();
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
        long test(int sendBandwidth,int sendMs) throws IOException, InterruptedException {
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
            return receiver.endTime-receiver.startTime;
        }
    }
    class TestThread extends Thread{
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
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
