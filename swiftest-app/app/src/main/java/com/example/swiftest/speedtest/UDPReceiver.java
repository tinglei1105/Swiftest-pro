package com.example.swiftest.speedtest;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;

// 只用于接收UDP，需要给定UDP socket
class UDPReceiver extends Thread {
    public long startTime;
    static String TAG="UDPReceiver";
    public long endTime=0;
    public long byteCount = 0;
    DatagramSocket datagramSocket;

    ArrayList<Long>timestamps;
    ArrayList<Long>byteCountRecords;
    long sampleInterval=10;
    long sampleWindow=50;

    public double speedMid=0;

    public UDPReceiver(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
        this.timestamps=new ArrayList<>();
        this.byteCountRecords=new ArrayList<>();
    }

    public void calculateSpeedMid(){
        ArrayList<Double>speedSample=new ArrayList<>();
        Log.d(TAG, timestamps.toString());
        int indexInterval= (int) (sampleWindow/sampleInterval);
        for(int i=0;i<timestamps.size()-indexInterval;i++){
            long byteRecv=byteCountRecords.get(i+indexInterval)-byteCountRecords.get(i);
            long duration=timestamps.get(i+indexInterval)-timestamps.get(i);
            speedSample.add( (double)byteRecv*8000/duration/1024/1024 );
        }
        Log.d(TAG, speedSample.toString());
        Collections.sort(speedSample);
        Log.d(TAG, speedSample.toString());
        speedMid=speedSample.get(speedSample.size()/2);
    }

    @Override
    public void run() {
        startTime = 0;
        int BUFFER_SIZE = 1024;
        byte[] receive_buf = new byte[BUFFER_SIZE * 2];
        DatagramPacket receive_packet = new DatagramPacket(receive_buf, receive_buf.length);
        try {
            datagramSocket.setSoTimeout(100);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        Thread t = Thread.currentThread();
        while (!t.isInterrupted()) {
            try {
                datagramSocket.receive(receive_packet);
                String receive_data = new String(receive_packet.getData(), 0, receive_packet.getLength());
                byteCount += receive_data.length();
                if (startTime == 0) {
                    startTime = System.currentTimeMillis();
                    Log.d("first packet arrived", String.valueOf(startTime));
                    timestamps.add(startTime);
                    byteCountRecords.add(byteCount);
                }
                endTime = System.currentTimeMillis();
                if(endTime-timestamps.get(timestamps.size()-1)>=sampleInterval){
                    timestamps.add(endTime);
                    byteCountRecords.add(byteCount);
                }

            } catch (IOException e) {
                //e.printStackTrace();
                calculateSpeedMid();
                return;
            }
            //Log.d(TAG, "run: continue to receive");
        }
        Log.d(TAG, "run: quit UDP receiver");
        calculateSpeedMid();
    }
}
