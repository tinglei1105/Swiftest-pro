package com.example.swiftest.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.swiftest.R;
import com.example.swiftest.databinding.FragmentHomeBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //final TextView textView = binding.textHome;
        //homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        TabLayout tabLayout=binding.tabTestMethod;
        ViewPager2 viewPager=binding.viewPagerTest;

        // https://juejin.cn/post/6844904128347389960
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position){
                    case 0:
                        return new FastTestFragment();
                    case 1:
                        return new BaselineTestFragment();
                    case 2:
                        return new PacketTrainFragment();
                }
                return new FastTestFragment();
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });

        TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                switch (position){
                    case 0:
                        tab.setText(R.string.tab_fast_test);
                        break;
                    case 1:
                        tab.setText(R.string.tab_baseline_test);
                    case 2:
                        tab.setText(R.string.tab_packet_train);
                }
            }
        });
        //要执行这一句才是真正将两者绑定起来
        mediator.attach();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}