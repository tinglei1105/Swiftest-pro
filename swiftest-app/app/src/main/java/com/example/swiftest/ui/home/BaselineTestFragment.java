package com.example.swiftest.ui.home;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.swiftest.R;
import com.example.swiftest.databinding.FragmentBaselineTestBinding;
import com.example.swiftest.speedtest.baseline.BaselineTester;
import com.example.swiftest.speedtest.TestResult;
import com.example.swiftest.ui.GaugeView;

import java.io.IOException;

public class BaselineTestFragment extends Fragment {

    private BaselineTestViewModel mViewModel;
    private FragmentBaselineTestBinding binding;
    public static BaselineTestFragment newInstance() {
        return new BaselineTestFragment();
    }
    BaselineTester baselineTester;
    private boolean isTesting=false;
    static String TAG="BaselineTestFragment";
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding=FragmentBaselineTestBinding.inflate(inflater,container,false);
        Button btn=binding.btnStart;
        View root=binding.getRoot();

        btn.setOnClickListener(v->{
            if(!isTesting){
                btn.setText(R.string.text_stop);

                baselineTester=new BaselineTester(getContext());
                DrawGuageThread drawGuageThread= new DrawGuageThread(binding.dlGauge,binding.dlText, baselineTester,binding.dlProgress);
                Thread testThread=new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            TestResult result=baselineTester.test();
                            Log.d(TAG, String.format("baseline: %f",result.bandwidth));
                            getActivity().runOnUiThread(()->{
                                binding.dlGauge.setValue(mbpsToGauge(result.bandwidth));
                                binding.dlText.setText(String.format("%.2f",result.bandwidth));
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        getActivity().runOnUiThread(()->{
                            btn.setText(R.string.text_start);
                        });
                        isTesting=false;
                    }
                });
                isTesting=true;
                drawGuageThread.start();
                testThread.start();
            }else{
                isTesting=false;
                baselineTester.stop();
                btn.setText(R.string.text_start);
            }
        });



        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(BaselineTestViewModel.class);
        // TODO: Use the ViewModel
    }
    private int mbpsToGauge(double s){
        return (int)(1000*(1-(1/(Math.pow(1.1,Math.sqrt(s))))));
    }
    class DrawGuageThread extends Thread{
        boolean finish;
        GaugeView gaugeView;
        BaselineTester baselineTester;
        TextView speedText;
        ProgressBar progressBar;
        String TAG="DrawGuage";
        DrawGuageThread(GaugeView gaugeView,TextView speedText,BaselineTester baselineTester,
                        ProgressBar progressBar){
            this.gaugeView=gaugeView;
            this.baselineTester=baselineTester;
            this.speedText=speedText;
            this.progressBar=progressBar;
            progressBar.setMax(10000);
        }

        @Override
        public void run() {
            long start_time=System.currentTimeMillis();
            getActivity().runOnUiThread(()->{
                speedText.setText("0.0");
                progressBar.setProgress(0);
            });
            gaugeView.setValue(0);
            while (!baselineTester.isStop()){
                try {
                    sleep(100);
                    int sz=baselineTester.speedSample.size();
                    if(sz>0){
                        double speed=baselineTester.speedSample.get(sz-1);
                        //Log.d(TAG, String.format("speed: %.2f guage %d",speed,mbpsToGauge(speed)));
                        gaugeView.setValue(mbpsToGauge(speed));
                        getActivity().runOnUiThread(()->{
                            speedText.setText(String.format("%.2f",speed));
                            progressBar.setProgress((int) (System.currentTimeMillis()-start_time));
                        });


                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}