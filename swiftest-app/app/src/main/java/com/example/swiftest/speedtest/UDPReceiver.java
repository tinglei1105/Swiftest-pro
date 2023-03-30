package com.example.swiftest.speedtest;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

// 只用于接收UDP，需要给定UDP socket
class UDPReceiver extends Thread {
    public long startTime;
    public long endTime;
    public long byteCount = 0;
    DatagramSocket datagramSocket;

    public UDPReceiver(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
    }

    @Override
    public void run() {
        startTime = 0;
        int BUFFER_SIZE = 1024;
        byte[] receive_buf = new byte[BUFFER_SIZE * 2];
        DatagramPacket receive_packet = new DatagramPacket(receive_buf, receive_buf.length);

        Thread t = Thread.currentThread();
        while (!t.isInterrupted()) {
            try {
                datagramSocket.receive(receive_packet);
                if (startTime == 0) {
                    startTime = System.currentTimeMillis();
                    Log.d("first packet arrived", String.valueOf(startTime));
                }
                endTime = System.currentTimeMillis();
                String receive_data = new String(receive_packet.getData(), 0, receive_packet.getLength());
                byteCount += receive_data.length();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
