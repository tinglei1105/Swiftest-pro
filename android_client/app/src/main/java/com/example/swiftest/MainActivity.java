package com.example.swiftest;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.example.swiftest.speedtest.BandwidthTestable;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Locale;

import  com.example.swiftest.speedtest.*;
public class MainActivity extends AppCompatActivity {
    boolean isTesting = false;
    BandwidthTestable bandwidthTest;
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
        Button button = findViewById(R.id.start);
        MyView myView = findViewById(R.id.my_view);
        //bandwidthTest=new NonFloodingTester(this);
        bandwidthTest= new FloodingTester(this);


        button.setOnClickListener(view -> {
            if (isTesting) {
                isTesting = false;
                button.setText(R.string.start);
                bandwidthTest.stop();
            } else {
                isTesting = true;
                //清空
                // bandwidthTest.speedSample.clear();

                button.setText(R.string.stop);
                bandwidth_text.setText(R.string.testing);
                duration_text.setText(R.string.testing);
                traffic_text.setText(R.string.testing);
                // flooding test专有，绘制图像
                if (bandwidthTest.getClass().equals(FloodingTester.class)){
                    FloodingTester tester = (FloodingTester)bandwidthTest;
                    tester.speedSample.clear();
                    myTestThread mtt = new myTestThread(tester.speedSample, myView);
                    mtt.start();
                }

                new Thread(() -> {
                    String bandwidth = "0";
                    String duration = "0";
                    String traffic = "0";
                    TestResult result;
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
                        isTesting = false;
                        button.setText(R.string.start);
                        bandwidth_text.setText(finalBandwidth);
                        duration_text.setText(finalDuration);
                        traffic_text.setText(finalTraffic);
                        // network_text.setText(finalNetwork);
                    });
                }).start();
                //for test

            }
        });
    }
}
