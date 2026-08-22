package com.audiovisualizer;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.audiovisualizer.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupUI();
    }

    private void setupUI() {
        // TODO: Initialize UI components
    }
}
