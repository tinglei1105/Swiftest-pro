package com.example.swiftest.speedtest;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

class Download extends Thread {
    DatagramSocket socket;
    String ip;
    int speed;
    int sendingTime;
    long dataSize; // speed*sendingTime
    long size;
    long receivingTime = 0;
    public ArrayList<Double> speedSample=new ArrayList<>();

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
            // Log.d("size changed", String.valueOf(size) + String.valueOf(lastSize));
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