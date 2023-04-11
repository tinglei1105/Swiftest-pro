package com.example.swiftest.speedtest;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * AdvancedClient can interact with advancedSender
 */
public class AdvancedClient {
    String key;//only key for the client
    InetAddress serverAddress;
    int tcpPort=9878;
    int udpPort=9877;
    DatagramSocket udpSock;
    Socket tcpSocket;
    int sendSpeed=200;//Mbps
    int sendDuration=3000;//ms
    static int BUFFER_SIZE=1024;
    static String TAG="AdvancedClient";
    public AdvancedClient(String key,String serverAddress){
        this.key=key;
        Log.d(TAG, serverAddress);
        try {
            this.serverAddress=InetAddress.getByName(serverAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void connect() throws IOException, InterruptedException {
        tcpSocket=new Socket(serverAddress,tcpPort);
        udpSock=new DatagramSocket();
        byte[] key_bytes=key.getBytes();
        tcpSocket.getOutputStream().write(key_bytes);
        Thread.sleep(10);

        DatagramPacket udpPacket=new DatagramPacket(key_bytes,key_bytes.length,serverAddress,udpPort);
        Thread sendTrigger=new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    for(int i=0;i<10;i++){
                        try {
                            udpSock.send(udpPacket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                        return;
                    }
                }
            }
        });

        sendTrigger.start();
        byte[] receiveBuf=new byte[BUFFER_SIZE*2];
        // 获取ACK
        int sz=tcpSocket.getInputStream().read(receiveBuf);
        Log.d("packet train controller", new String(receiveBuf,0,sz));
        sendTrigger.interrupt();
    }

    public void startSend(int speed,int duration) throws IOException {
        String message=String.format("%d,%d;",speed,duration);
        tcpSocket.getOutputStream().write(message.getBytes());
    }
    public void stopSend() throws IOException {
        String message="STOP;";
        tcpSocket.getOutputStream().write(message.getBytes());
    }
    public int getUsage() throws IOException {
        String message="USAGE;";
        tcpSocket.getOutputStream().write(message.getBytes());
        Log.d(TAG, "getUsage: ");
        while(true){
            byte[] receiveBuf=new byte[BUFFER_SIZE*2];
            int sz=tcpSocket.getInputStream().read(receiveBuf);
            if(sz<=0)continue;
            String resp=new String(receiveBuf,0,sz);
            Log.d(TAG, resp);
            int idx= resp.indexOf("USAGE");
            if(idx==-1)continue;
            String usageStr=resp.substring(idx+6);
            return Integer.parseInt(usageStr);
        }
    }
    public void end() throws IOException {
        String message="END;";
        tcpSocket.getOutputStream().write(message.getBytes());
    }
}
