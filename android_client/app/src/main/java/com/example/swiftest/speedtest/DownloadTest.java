package com.example.swiftest.speedtest;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;

public class DownloadTest {
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
            Download download = new Download(serverIP.get(i), ctlSocket.get(i),eachServerSpeed, sendingTime);
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
            String finMessage=String.format(Locale.CHINA,"FIN-%.2f",trafficMB*1024*1024);
            outputStreams.get(i).write(finMessage.getBytes());
            ctlSocket.get(i).close();
        }
        return (sendingTime/(double)recvTime)*sendSpeed;
    }

    public double getTrafficMB() {
        return trafficMB;
    }
}
