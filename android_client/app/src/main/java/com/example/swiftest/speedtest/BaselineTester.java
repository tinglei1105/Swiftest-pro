package com.example.swiftest.speedtest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class BaselineTester implements BandwidthTestable{
    Context context;
    String TAG="BaselineTester";
    private boolean stop=false;
    ArrayList<String>ipList;
    long startTime;
    public BaselineTester(Context context,ArrayList<String>ipList){
        this.context=context;
        this.ipList=ipList;
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
        ArrayList<TCPDownload>downloadThreads=new ArrayList<>();
        for(int i=0;i<4&&i<ipList.size();i++){
            downloadThreads.add(new TCPDownload(ipList.get(i)));
            downloadThreads.get(i).start();
        }
        int total_size=0;
        for(int i=0;i<4&&i<ipList.size();i++){
            downloadThreads.get(i).join();
            total_size+=downloadThreads.get(i).size;
        }

        long duration =System.currentTimeMillis()-startTime;
        Log.d(TAG, String.format("download size:%d, cost:%d",total_size,duration));
        return new TestResult((double) total_size/1024/1024*8000/duration,0,0);
    }
    public long getStartTime(){
        return  startTime;
    }
    @Override
    public void stop() {
        this.stop=true;
    }
}
