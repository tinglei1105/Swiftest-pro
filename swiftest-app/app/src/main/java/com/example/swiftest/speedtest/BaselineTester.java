package com.example.swiftest.speedtest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

public class BaselineTester implements BandwidthTestable{
    Context context;
    String TAG="BaselineTester";
    private boolean stop=false;
    ArrayList<String>ipList;
    ArrayList<Double>speedSample;
    ArrayList<TCPDownload>downloadThreads;
    static int interval=100;
    static int checkwindow=1000;
    long startTime;
    public BaselineTester(Context context,ArrayList<String>ipList){
        this.context=context;
        this.ipList=ipList;
        speedSample=new ArrayList<>();
    }
    static class SampleThread extends Thread{
        public ArrayList<Double>speedSample;
        public ArrayList<TCPDownload>downloads;
        public ArrayList<Double>sizeRecords;
        double preSize=0;
        public SampleThread(ArrayList<Double> speedSample,ArrayList<TCPDownload>downloads){
            this.speedSample=speedSample;
            this.downloads=downloads;
            this.sizeRecords=new ArrayList<>();
        }
        @Override
        public void run() {
            while (true){
                try {
                    sleep(interval);
                    double total=0.0;
                    for(TCPDownload download:downloads){
                        total+=download.size;
                    }
                    sizeRecords.add(total);
                    int i=checkwindow/interval;
                    if(i>sizeRecords.size())continue;
                    int sz=sizeRecords.size();
                    speedSample.add((sizeRecords.get(sz-1)-sizeRecords.get(sz-i))/checkwindow*1000*8/1024/1024);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }

        }
    }
    @Override
    public TestResult test() throws IOException, InterruptedException {
        stop=false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d("No permission:", "ACCESS_FINE_LOCATION");

                // TODO: throw an error?
                return new TestResult();
            }
        }
        startTime = System.currentTimeMillis();
        //ArrayList<String>ipList=new ArrayList<>(Collections.singleton("192.168.31.247"));
        downloadThreads=new ArrayList<>();
        int server_num=5;
        for(int i=0;i<server_num&&i<ipList.size();i++){
            downloadThreads.add(new TCPDownload(ipList.get(i)));
            downloadThreads.get(i).start();
        }
        SampleThread sampleThread=new SampleThread(speedSample,downloadThreads);
        sampleThread.start();

        int total_size=0;
        for(int i=0;i<server_num&&i<ipList.size();i++){
            downloadThreads.get(i).join();
            total_size+=downloadThreads.get(i).size;
        }
        sampleThread.interrupt();
        long duration =System.currentTimeMillis()-startTime;
        Log.d(TAG, String.format("download size:%d, cost:%d",total_size,duration));
        Log.d(TAG,speedSample.toString());
        if(stop){
            throw new InterruptedException();
        }
        return new TestResult((double) total_size/1024/1024*8000/duration,0,0,0);
    }
    public long getStartTime(){
        return  startTime;
    }
    @Override
    public void stop() {
        this.stop=true;
        if(downloadThreads==null)return;
        for(TCPDownload download:downloadThreads){
            download.finish();
        }
    }
}
