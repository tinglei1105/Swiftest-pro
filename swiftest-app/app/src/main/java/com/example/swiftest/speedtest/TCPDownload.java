package com.example.swiftest.speedtest;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TCPDownload extends Thread{
    String ip;
    long size;
    long duration;
    private boolean stop=false;
    String TAG="TCPDownload";
    TCPDownload(String ip){
        this.ip=ip;
    }
    public void finish(){
        stop=true;
    }
    public void run() {
        size=0;
        try {
            URL url = new URL("http://" + ip + ":8080/static/GB");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int timeout=10000;
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            long nowTime = System.currentTimeMillis();
            connection.connect();

            InputStream is = connection.getInputStream();

            byte[] receive_buf = new byte[2048];
            int len;
            while((len=is.read(receive_buf))!=-1 && !stop){
                size+=len;
                if(System.currentTimeMillis()-nowTime>timeout){
                    connection.disconnect();
                    long endTime=System.currentTimeMillis();
                    duration=endTime-nowTime;
                    Log.d(TAG, String.format("receive: %.2f, time: %.2f",(double)size/1024/1024,(double)(endTime-nowTime)/1000));
                }
            }
            long endTime=System.currentTimeMillis();
            duration=endTime-nowTime;
            Log.d(TAG, String.format("receive: %.2f, time: %.2f",(double)size/1024/1024,(double)(endTime-nowTime)/1000));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
//    public void run() {
//        int BUFFER_SIZE = 1024;
//        size=0;
//        byte[] receive_buf = new byte[BUFFER_SIZE];
//
//        try {
//            socket=new Socket(ip,9876);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try {
//            InputStream is=socket.getInputStream();
//            while (true){
//                int recv=is.read(receive_buf);
//                if (recv == -1){
//                    Log.d(TAG, "tcp disconnected");
//                    break;
//                }
//                size+=recv;
//            }
//            Log.d(TAG, String.format("final size: %d",size));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
