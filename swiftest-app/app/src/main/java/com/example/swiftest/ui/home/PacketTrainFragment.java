package com.example.swiftest.ui.home;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.swiftest.R;
import com.example.swiftest.databinding.FragmentPacketTrainBinding;
import com.example.swiftest.speedtest.AdvancedFloodingTester;
import com.example.swiftest.speedtest.PacketTrainTester;
import com.example.swiftest.speedtest.TestResult;
import com.example.swiftest.speedtest.TestUtil;

import java.io.IOException;

public class PacketTrainFragment extends Fragment {

    private PacketTrainViewModel mViewModel;
    private FragmentPacketTrainBinding binding;

    public static PacketTrainFragment newInstance() {
        return new PacketTrainFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding=FragmentPacketTrainBinding.inflate(inflater,container,false);
        binding.packetTrainBtn.setOnClickListener(v->{
            new Thread(()->{
                PacketTrainTester tester=new PacketTrainTester(getContext());
                getActivity().runOnUiThread(
                        ()->{
                            binding.resultText.setText("");
                        }
                );

                tester.setOnTestListener(new PacketTrainTester.TestListener() {
                    @Override
                    public void process(String output) {
                        getActivity().runOnUiThread(()->{
                            //binding.resultText.setText(output);
                            binding.resultText.append(output);

                        });
                    }
                });
                try {
                    TestResult result=tester.test();
                    getActivity().runOnUiThread(
                            ()->{
                                binding.packetTrainBtn.setText(R.string.text_start);
                                //binding.resultText.setText(String.format("bandwidth:%.2f\nduration:%.2f\ntraffic:%.2f",result.bandwidth,result.duration,result.traffic));
                                binding.resultText.append(String.format("bandwidth:%.2f\nduration:%.2f\ntraffic:%.2f",result.bandwidth,result.duration,result.traffic));
                            }
                    );

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            binding.packetTrainBtn.setText(R.string.text_stop);
        });

        binding.packetTrainSwiftestBtn.setOnClickListener(v->{
            new Thread(()->{
                PacketTrainTester tester=new PacketTrainTester(getContext());
                getActivity().runOnUiThread(
                        ()->{
                            binding.resultText.setText("");
                        }
                );

                tester.setOnTestListener(new PacketTrainTester.TestListener() {
                    @Override
                    public void process(String output) {
                        getActivity().runOnUiThread(()->{
                            //binding.resultText.setText(output);
                            binding.resultText.append(output);

                        });
                    }
                });
                AdvancedFloodingTester advancedFloodingTester=new AdvancedFloodingTester(getContext());
                try {
                    TestResult result=tester.test();

                    getActivity().runOnUiThread(()->{
                        binding.resultText.append(String.format("bandwidth:%.2f\nduration:%.2f\ntraffic:%.2f\n",result.bandwidth,result.duration,result.traffic));
                        binding.resultText.append("Start Swiftest");
                    });
                    TestResult swiftest=advancedFloodingTester.test();
                    getActivity().runOnUiThread(
                            ()->{
                                binding.packetTrainBtn.setText(R.string.text_start);
                                //binding.resultText.setText(String.format("bandwidth:%.2f\nduration:%.2f\ntraffic:%.2f",result.bandwidth,result.duration,result.traffic));
                                binding.resultText.append(String.format("bandwidth:%.2f\nduration:%.2f\ntraffic:%.2f\n",swiftest.bandwidth,swiftest.duration,swiftest.traffic));
                            }
                    );
                    TestUtil.uploadPacketTrainBaseline(result,swiftest);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            binding.packetTrainBtn.setText(R.string.text_stop);
        });
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(PacketTrainViewModel.class);
        // TODO: Use the ViewModel
    }

}