package com.example.swiftest.ui.home;

import androidx.lifecycle.ViewModelProvider;

import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.transition.Scene;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.example.swiftest.R;
import com.example.swiftest.databinding.FragmentFastTestBinding;
import com.example.swiftest.speedtest.FloodingTester;
import com.example.swiftest.speedtest.MyNetworkInfo;
import com.example.swiftest.speedtest.NetworkUtil;
import com.example.swiftest.speedtest.TestResult;
import com.example.swiftest.speedtest.TestUtil;
import com.example.swiftest.ui.SampleView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class FastTestFragment extends Fragment {

    private FastTestViewModel mViewModel;
    private FragmentFastTestBinding binding;
    private boolean isTesting = false;
    private String TAG = "FastTestFragment";
    SampleView sampleView;
    FloodingTester floodingTester;

    public static FastTestFragment newInstance() {
        return new FastTestFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFastTestBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        sampleView = binding.sampleView;
        //root.removeView(sampleView);


        Button btn = binding.btnStart;
        btn.setOnClickListener(v -> {
            if (!isTesting) {
                binding.resultLayout.setVisibility(View.VISIBLE);
                btn.setText(R.string.text_stop);
                floodingTester = new FloodingTester(getContext());
                Thread drawSampleThread = new DrawSampleThread(floodingTester.speedSample, sampleView);
                Thread testThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            showTestingUI();
                            TestResult result = floodingTester.test();
                            showResult(result);
                            List<MyNetworkInfo.CellInfo> cellInfo = NetworkUtil.getCellInfo(getContext());
                            MyNetworkInfo.WifiInfo wifiInfo = NetworkUtil.getWifiInfo(getContext());
                            MyNetworkInfo networkInfo = new MyNetworkInfo(String.valueOf(Build.VERSION.SDK_INT), NetworkUtil.getNetworkType(getContext()), cellInfo, wifiInfo);
                            TestUtil.uploadTestResult(result, null, floodingTester.speedSample, networkInfo);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        isTesting = false;
                        requireActivity().runOnUiThread(() -> {
                            btn.setText(R.string.text_start);
                        });

                    }
                });
                drawSampleThread.start();
                testThread.start();
            } else {
                binding.resultLayout.setVisibility(View.GONE);
                btn.setText(R.string.text_start);
            }
            isTesting = !isTesting;

        });
        return root;
    }

    private void showResult(TestResult result) {
        String bandwidth = String.format(Locale.CHINA, "%.2f", result.bandwidth);
        String duration = String.format(Locale.CHINA, "%.2f", result.duration);
        String traffic = String.format(Locale.CHINA, "%.2f", result.traffic);


        getActivity().runOnUiThread(() -> {

            binding.bandwidth.setText(bandwidth);
            binding.duration.setText(duration);
            binding.traffic.setText(traffic);

        });
    }

    private void showTestingUI(){
        getActivity().runOnUiThread(() -> {
            binding.bandwidth.setText(R.string.text_testing);
            binding.duration.setText(R.string.text_testing);
            binding.traffic.setText(R.string.text_testing);

        });
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(FastTestViewModel.class);
        // TODO: Use the ViewModel
    }

    class DrawSampleThread extends Thread {
        boolean finish;
        int current_index;
        ArrayList<Double> speedSample;
        SampleView sampleView;

        DrawSampleThread(ArrayList<Double> speedSample, SampleView myView) {
            this.finish = false;
            this.current_index = 0;
            this.speedSample = speedSample;
            this.sampleView = myView;
        }

        public void run() {
            while (!finish) {
                try {
                    sleep(50);
                    current_index++;
                    StringBuilder sb = new StringBuilder();
                    for (int i = speedSample.size() - 1; i >= 0; i--) {
                        double num = speedSample.get(i);
                        sb.append(num);
                        sb.append(",");
                    }
                    this.sampleView.setSpeedSamples(speedSample);
                    this.sampleView.invalidate();
                    if (!isTesting) break;
                } catch (InterruptedException e) {
                    Log.d("test_thread", "bug");
                }
                //draw on windows
            }
        }

    }

    public void startTest(View view) {
        Log.d(TAG, "startTest: ");
    }
}