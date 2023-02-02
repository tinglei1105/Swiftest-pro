package com.example.swiftest.ui.configurations;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.swiftest.MainActivity;
import com.example.swiftest.databinding.FragmentConfigurationsBinding;
import com.hjq.language.MultiLanguages;

import java.util.Locale;


public class ConfigurationsFragment extends Fragment {

    private FragmentConfigurationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ConfigurationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(ConfigurationsViewModel.class);

        binding = FragmentConfigurationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RadioGroup radioGroup = binding.languageRadios;
        Locale locale = MultiLanguages.getAppLanguage();
        if (Locale.CHINA.equals(locale)) {
            radioGroup.check(binding.languageRadioZh.getId());
        } else  {
            radioGroup.check(binding.languageRadioEn.getId());
        }
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // 是否需要重启
                boolean restart = false;
                if(checkedId==binding.languageRadioZh.getId()){
                    restart=MultiLanguages.setAppLanguage(getActivity(),Locale.CHINA);
                }else{
                    restart=MultiLanguages.setAppLanguage(getActivity(),Locale.ENGLISH);
                }

                if(restart){
                    startActivity(new Intent(getActivity(), MainActivity.class));
                }
            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}