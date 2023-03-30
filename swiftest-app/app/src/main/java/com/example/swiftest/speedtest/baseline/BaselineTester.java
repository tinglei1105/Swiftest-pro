package com.example.swiftest.speedtest.baseline;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.blankj.utilcode.util.NetworkUtils;
import com.example.swiftest.speedtest.BandwidthTestable;
import com.example.swiftest.speedtest.IPListGetter;
import com.example.swiftest.speedtest.NetworkUtil;
import com.example.swiftest.speedtest.TestResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

public class BaselineTester implements BandwidthTestable {
    Context context;
    String TAG="BaselineTester";
    private boolean stop=false;
    ArrayList<String>ipList;
    public ArrayList<Double>speedSample;
    ArrayList<TCPDownload>downloadThreads;
    static int interval=50;
    static int checkwindow=2000;
    long startTime;
    String networkType;
    String client_ip;
    public BaselineTester(Context context){
        this.context=context;
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
                    for(TCPDownload downloadThread:downloads){
                        downloadThread.finish();
                    }
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
        networkType = NetworkUtil.getNetworkType(context);
        IPListGetter ipListGetter = new IPListGetter();
        ipListGetter.start();
        ipListGetter.join();
        ipList = ipListGetter.getIpList();
        client_ip = ipListGetter.client_ip;
        //ArrayList<String>ipList=new ArrayList<>(Collections.singleton("192.168.31.247"));
        downloadThreads=new ArrayList<>();
        int server_num=5;
        for(int i=0;i<server_num&&i<ipList.size();i++){
            downloadThreads.add(new TCPDownload(ipList.get(i)));
            downloadThreads.get(i).start();
        }
        SampleThread sampleThread=new SampleThread(speedSample,downloadThreads);
        sampleThread.start();
        TimerTask timerTask=new TimerTask() {
            @Override
            public void run() {
                sampleThread.interrupt();
            }
        };
        new Timer().schedule(timerTask,10000);

        int total_size=0;
        for(int i=0;i<server_num&&i<ipList.size();i++){
            //downloadThreads.get(i).finish();
            downloadThreads.get(i).join();
            total_size+=downloadThreads.get(i).size;
        }
        //sampleThread.interrupt();
        long duration =System.currentTimeMillis()-startTime;
        Log.d(TAG, String.format("download size:%d, cost:%d",total_size,duration));
        Log.d(TAG,speedSample.toString());
        if(stop){
            throw new InterruptedException();
        }
        stop=true;
        ArrayList<Double> result_list=new ArrayList<>(speedSample.subList(0,speedSample.size()));
        Collections.sort(result_list);
        TestResult result = TestResult.builder().withBandwidth(result_list.get(result_list.size()/2)).
                withNetworkType(networkType).
                withNetworkOperator(NetworkUtils.getNetworkOperatorName()).
                withPrivateIP(NetworkUtils.getIPAddress(true)).
                withPublicIP(client_ip).build();
        Log.d(TAG, result.toString());
        return result;
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

    public boolean isStop(){
        return stop;
    }
}
