package com.example.swiftest;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.health.TimerStat;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.swiftest.speedtest.BandwidthTestable;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import  com.example.swiftest.speedtest.*;
public class MainActivity extends AppCompatActivity {
    boolean isTesting = false;
    FloodingTester bandwidthTest;
    BaselineTester baselineTester;
    TestResult baseline;
    TestResult result;
    progressThread pt;
    //for test
    class myTestThread extends Thread {
//        ArrayList<Double> speedSample;
        boolean finish;
        int current_index;
        ArrayList<Double> speedSample;
        MyView myView;

        myTestThread(ArrayList<Double> speedSample, MyView myView) {
            this.finish = false;
            this.current_index = 0;
            this.speedSample = speedSample;
            this.myView = myView;
        }

        public void run() {
            while (!finish) {
                try {
                    sleep(50);
                    current_index ++;
                    StringBuilder sb = new StringBuilder();
                    for (int i = speedSample.size() - 1; i >= 0; i--) {
                        double num = speedSample.get(i);
                        sb.append(num);
                        sb.append(",");
                    }
                    String result = sb.toString();

//                    Log.d("!!!!!!!!!!!!!!!!! size:", Integer.toString(this.speedSample.size()));
                    this.myView.setSpeedSamples(speedSample);
                    this.myView.invalidate();
                    if(!isTesting) break;
                } catch (InterruptedException e) {
                    Log.d("test_thread", "bug");
                }

                //draw on windows
            }
        }

    }
    class progressThread extends Thread{
        ProgressBar progressBar;
        BaselineTester baselineTester;
        public boolean finished=false;
        progressThread(ProgressBar progressBar,BaselineTester baselineTester){
            this.progressBar=progressBar;
            this.baselineTester=baselineTester;
        }
        @Override
        public void run() {
            if(baselineTester==null)return;
            while(!finished){
                //Log.d("progress", String.format("time :%d",System.currentTimeMillis()-baselineTester.getStartTime()));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress((int) (System.currentTimeMillis()-baselineTester.getStartTime()));
                    }
                });
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // setContentView(new MyView(this));
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        Logger.addLogAdapter(new AndroidLogAdapter());

        TextView bandwidth_text = findViewById(R.id.bandwidth);
        TextView duration_text = findViewById(R.id.duration);
        TextView traffic_text = findViewById(R.id.traffic);
        TextView baseline_text = findViewById(R.id.baseline);
        ProgressBar progressBar=findViewById(R.id.progress_bar);
        Button button = findViewById(R.id.start);
        MyView myView = findViewById(R.id.my_view);
        //bandwidthTest=new NonFloodingTester(this);
        //bandwidthTest= new FloodingTester(this);


        button.setOnClickListener(view -> {
            if (isTesting) {
                isTesting = false;
                button.setText(R.string.start);
                bandwidthTest.stop();
                baselineTester.stop();
                pt.finished=true;
                progressBar.setProgress(0);
            } else {
                isTesting = true;
                //清空
                // bandwidthTest.speedSample.clear();
                progressBar.setProgress(0);
                button.setText(R.string.stop);
                baseline_text.setText("Testing");
                IPListGetter ipListGetter=new IPListGetter();
                ipListGetter.start();
                try {
                    ipListGetter.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                baselineTester=new BaselineTester(this,ipListGetter.getIpList());
                pt=new progressThread(progressBar,baselineTester);
                pt.start();


                bandwidthTest=new FloodingTester(this,ipListGetter.getIpList());
                bandwidth_text.setText(R.string.testing);
                duration_text.setText(R.string.testing);
                traffic_text.setText(R.string.testing);


                    myTestThread mtt = new myTestThread(bandwidthTest.speedSample, myView);
                    mtt.start();

                Thread test_thread= new Thread(() -> {
                    String bandwidth = "0";
                    String duration = "0";
                    String traffic = "0";

                    try {
                        result=bandwidthTest.test();
                        bandwidth = String.format(Locale.CHINA,"%.2f",result.bandwidth);
                        duration = String.format(Locale.CHINA,"%.2f",result.duration);
                        traffic = String.format(Locale.CHINA,"%.2f",result.traffic);
                        // network = bandwidthTest.networkType;
                        bandwidthTest.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String finalBandwidth = bandwidth + "  Mbps";
                    String finalDuration = duration + "  s";
                    String finalTraffic = traffic + "  MB";
                    // String finalNetwork = network;
                    runOnUiThread(() -> {

                        bandwidth_text.setText(finalBandwidth);
                        duration_text.setText(finalDuration);
                        traffic_text.setText(finalTraffic);
                        // network_text.setText(finalNetwork);
                    });

                    try {

                        baseline=baselineTester.test();
                        runOnUiThread(()->{
                            baseline_text.setText(String.format(Locale.CHINA,"%.2f Mbps",baseline.bandwidth));
                        });

                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                        baseline_text.setText("Failed");
                    }
                    pt.finished=true;
                    List<MyNetworkInfo.CellInfo> cellInfo = NetworkUtil.getCellInfo(this);
                    MyNetworkInfo.WifiInfo wifiInfo = NetworkUtil.getWifiInfo(this);
                    //cellInfo.add(new MyNetworkInfo.CellInfo("",new MyNetworkInfo.CellInfo.CellIdentityCdma("","","","",""),null));
                    Log.d("cell info",cellInfo.toString());
                    Log.d("wifi info",wifiInfo.toString());
                    TestUtil.uploadTestResult(result,baseline,bandwidthTest.speedSample,new MyNetworkInfo(String.valueOf(Build.VERSION.SDK_INT),NetworkUtil.getNetworkType(this),cellInfo,wifiInfo));
                    runOnUiThread(()->{
                        isTesting = false;
                        button.setText(R.string.start);
                    });
                });
                test_thread.start();

            }
        });

    }
}
